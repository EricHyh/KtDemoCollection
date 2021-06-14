package com.hyh.list.internal

import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.IItemSource
import com.hyh.list.IParamProvider
import com.hyh.list.ItemSourceInfo
import com.hyh.list.ItemSourceRepository
import com.hyh.tabs.TabSource
import com.hyh.tabs.internal.CacheTabLoader
import com.hyh.tabs.internal.TabEvent
import com.hyh.tabs.internal.TabFetcherSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*


abstract class ItemSourceFetcher<Param : Any>(private val initialParam: Param?) {

    private val uiReceiver = object : UiReceiverForRepo<Param> {

        private val state = MutableStateFlow(Pair<Long, Param?>(0, initialParam))

        val flow = state
            .map { it.second }

        override fun refresh(param: Param) {
            state.value = Pair(state.value.first + 1, param)
        }
    }

    val flow: Flow<RepoData<Param>> = simpleChannelFlow<RepoData<Param>> {
        uiReceiver
            .flow
            .onStart {
                emit(initialParam)
            }
            .simpleScan(null) { previousSnapshot: ItemSourceFetcherSnapshot<Param>?, param: Param? ->
                previousSnapshot?.close()
                val completeTimes = (previousSnapshot?.completeTimes ?: 0) + (if (previousSnapshot?.getCacheComplete == true) 1 else 0)
                val snapshot: ItemSourceFetcherSnapshot<Param> = if (param == null) {
                    ItemSourceFetcherSnapshot(param, completeTimes, getCacheLoader(), getLoader(), null)
                } else {
                    ItemSourceFetcherSnapshot(param, completeTimes, getCacheLoader(), getLoader(), getFetchDispatcher(param))
                }
                snapshot
            }
            .filterNotNull()
            .simpleMapLatest { snapshot ->
                val downstreamFlow = snapshot.repoEventFlow
                RepoData(downstreamFlow, uiReceiver)
            }
            .collect {
                send(it)
            }
    }.buffer(Channel.BUFFERED)

    private fun getCacheLoader(): CacheSourceLoader<Param> = ::getCache

    private fun getLoader(): SourceLoader<Param> = ::load

    abstract suspend fun getCache(param: Param, completeTimes: Int): ItemSourceRepository.CacheResult

    abstract suspend fun load(param: Param): ItemSourceRepository.LoadResult

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher

}

class ItemSourceFetcherSnapshot<Param : Any>(
    private val param: Param?,
    val completeTimes: Int,
    private val cacheLoader: CacheSourceLoader<Param>,
    private val loader: SourceLoader<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
) {

    var getCacheComplete = false

    private val repoEventChannelFlowJob = Job()
    private val repoEventCh = Channel<RepoEvent>(Channel.BUFFERED)

    val repoEventFlow: Flow<RepoEvent> = cancelableChannelFlow(repoEventChannelFlowJob) {
        launch {
            repoEventCh.consumeAsFlow().collect {
                // Protect against races where a subsequent call to submitData invoked close(),
                // but a tabEvent arrives after closing causing ClosedSendChannelException.
                try {
                    send(it)
                } catch (e: ClosedSendChannelException) {
                    // Safe to drop tabEvent here, since collection has been cancelled.
                }
            }
        }

        if (param == null) {
            return@cancelableChannelFlow
        }

        repoEventCh.send(RepoEvent.Loading)

        val cacheResult = cacheLoader.invoke(param, completeTimes)
        var usingCache = false
        if (cacheResult is ItemSourceRepository.CacheResult.Success) {
            usingCache = true
            val sources = newSources(cacheResult.sources)
            val event = RepoEvent.UsingCache(sources)
            repoEventCh.send(event)
            getCacheComplete = true
        }

        val loadResult: ItemSourceRepository.LoadResult
        if (fetchDispatcher == null) {
            loadResult = loader.invoke(param)
        } else {
            withContext(fetchDispatcher) {
                loadResult = loader.invoke(param)
            }
        }
        when (loadResult) {
            is ItemSourceRepository.LoadResult.Success -> {
                val sources = newSources(loadResult.sources)
                val event = RepoEvent.Success(sources)
                repoEventCh.send(event)
            }
            is ItemSourceRepository.LoadResult.Error -> {
                val event = RepoEvent.Error(loadResult.error, usingCache)
                repoEventCh.send(event)
            }
        }
    }

    fun close() {
        repoEventChannelFlowJob.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun newSources(sources: List<ItemSourceInfo>): List<LazySourceData<Any>> {
        return sources
            .map {
                val sourceToken: Any = it.sourceToken
                val paramProvider: IParamProvider<Any> = it.paramProvider as IParamProvider<Any>
                val lazyFlow: Deferred<Flow<SourceData<Any>>> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
                    ItemFetcher(
                        it.lazySource.value as IItemSource<Any>,
                        paramProvider.getParam()
                    ).flow
                }
                LazySourceData(sourceToken, paramProvider, lazyFlow)
            }
    }
}

internal typealias CacheSourceLoader<Param> = (suspend (param: Param, completeTimes: Int) -> ItemSourceRepository.CacheResult)
internal typealias SourceLoader<Param> = (suspend (param: Param) -> ItemSourceRepository.LoadResult)