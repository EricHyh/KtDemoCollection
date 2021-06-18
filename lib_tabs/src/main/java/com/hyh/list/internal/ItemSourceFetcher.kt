package com.hyh.list.internal

import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.*
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
                ItemSourceFetcherSnapshot(
                    param,
                    previousSnapshot?.cacheResult,
                    previousSnapshot?.loadResult,
                    getCacheLoader(),
                    geOnSourceCacheResult(),
                    getLoader(),
                    geOnSourceLoadResult(),
                    if (param == null) Dispatchers.Unconfined else getFetchDispatcher(param)
                )
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

    private fun getCacheLoader(): SourceCacheLoader<Param> = ::getCache
    private fun geOnSourceCacheResult(): OnSourceCacheResult<Param> = ::onCacheResult

    private fun getLoader(): SourceLoader<Param> = ::load
    private fun geOnSourceLoadResult(): OnSourceLoadResult<Param> = ::onLoadResult

    abstract suspend fun getCache(params: ItemSourceRepository.CacheParams<Param>): ItemSourceRepository.CacheResult
    abstract suspend fun onCacheResult(params: ItemSourceRepository.CacheParams<Param>, cacheResult: ItemSourceRepository.CacheResult)

    abstract suspend fun load(params: ItemSourceRepository.LoadParams<Param>): ItemSourceRepository.LoadResult
    abstract suspend fun onLoadResult(params: ItemSourceRepository.LoadParams<Param>, loadResult: ItemSourceRepository.LoadResult)

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher

}

class ItemSourceFetcherSnapshot<Param : Any>(
    private val param: Param?,
    private val lastCacheResult: ItemSourceRepository.CacheResult? = null,
    private val lastLoadResult: ItemSourceRepository.LoadResult? = null,
    private val cacheLoader: SourceCacheLoader<Param>,
    private val onSourceCacheResult: OnSourceCacheResult<Param>,
    private val loader: SourceLoader<Param>,
    private val onSourceLoadResult: OnSourceLoadResult<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
) {

    var cacheResult: ItemSourceRepository.CacheResult? = null
    var loadResult: ItemSourceRepository.LoadResult? = null

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

        val cacheParams = ItemSourceRepository.CacheParams(param, lastCacheResult, lastLoadResult)
        val cacheResult = cacheLoader.invoke(cacheParams)
        this@ItemSourceFetcherSnapshot.cacheResult = cacheResult
        var usingCache = false
        if (cacheResult is ItemSourceRepository.CacheResult.Success) {
            usingCache = true
            val sources = newSources(cacheResult.sources)
            val event = RepoEvent.UsingCache(sources)
            repoEventCh.send(event)
        }
        onSourceCacheResult(cacheParams, cacheResult)

        val loadParams = ItemSourceRepository.LoadParams(param, lastCacheResult, lastLoadResult)
        val loadResult: ItemSourceRepository.LoadResult
        if (fetchDispatcher == null) {
            loadResult = loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loadResult = loader.invoke(loadParams)
            }
        }
        this@ItemSourceFetcherSnapshot.loadResult = loadResult
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
        onSourceLoadResult(loadParams, loadResult)
    }

    fun close() {
        repoEventChannelFlowJob.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun newSources(sources: List<ItemSourceRepository.ItemSourceInfo>): List<LazySourceData<Any>> {
        return sources
            .map {
                val sourceToken: Any = it.sourceToken
                val paramProvider: IParamProvider<Any> = it.paramProvider as IParamProvider<Any>
                val lazyFlow: Deferred<Flow<SourceData<Any>>> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
                    ItemFetcher(
                        it.lazySource.value as ItemSource<Any>,
                        paramProvider.getParam()
                    ).flow
                }
                LazySourceData(sourceToken, paramProvider, lazyFlow)
            }
    }
}

internal typealias SourceCacheLoader<Param> = (suspend (ItemSourceRepository.CacheParams<Param>) -> ItemSourceRepository.CacheResult)
internal typealias OnSourceCacheResult<Param> = (suspend (ItemSourceRepository.CacheParams<Param>, ItemSourceRepository.CacheResult) -> Unit)
internal typealias SourceLoader<Param> = (suspend (ItemSourceRepository.LoadParams<Param>) -> ItemSourceRepository.LoadResult)
internal typealias OnSourceLoadResult<Param> = (suspend (ItemSourceRepository.LoadParams<Param>, ItemSourceRepository.LoadResult) -> Unit)