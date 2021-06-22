package com.hyh.list.internal

import android.os.SystemClock
import com.hyh.Invoke
import com.hyh.RefreshActuator
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlin.math.abs


abstract class ItemSourceFetcher<Param : Any>(private val initialParam: Param?) {

    private val uiReceiver = object : UiReceiverForRepo<Param> {

        private val state = MutableStateFlow(Pair<Long, Param?>(0, initialParam))

        private var cacheState: MutableStateFlow<Pair<Long, Param>>? = null
        private var refreshStage = RefreshStage.UNBLOCK
        private var delay = 0
        private var timingStart: Long = 0

        val flow = state.map { it.second }

        override fun refresh(param: Param) {
            when (refreshStage) {
                RefreshStage.UNBLOCK -> {
                    state.value = Pair(state.value.first + 1, param)
                    when (val refreshStrategy = getRefreshStrategy()) {
                        is RefreshStrategy.QueueUp -> {
                            refreshStage = RefreshStage.BLOCK
                        }
                        is RefreshStrategy.DelayedQueueUp -> {
                            refreshStage = RefreshStage.TIMING
                            timingStart = SystemClock.elapsedRealtime()
                            delay = refreshStrategy.delay
                        }
                        else -> {
                            refreshStage = RefreshStage.UNBLOCK
                        }
                    }
                }
                RefreshStage.TIMING -> {
                    state.value = Pair(state.value.first + 1, param)
                    val elapsedRealtime = SystemClock.elapsedRealtime()
                    if (abs(elapsedRealtime - timingStart) > delay) {
                        refreshStage = RefreshStage.BLOCK
                    }
                }
                RefreshStage.BLOCK -> {
                    val cacheState = this.cacheState
                    if (cacheState != null) {
                        cacheState.value = Pair(cacheState.value.first + 1, param)
                    } else {
                        this.cacheState = MutableStateFlow(Pair<Long, Param>(0, param))
                    }
                }
            }
        }

        fun onRefreshComplete() {
            val cacheState = this.cacheState
            this.cacheState = null
            timingStart = 0
            refreshStage = RefreshStage.UNBLOCK
            if (cacheState != null) {
                refresh(cacheState.value.second)
            }
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
                    geOnCacheResult(),
                    getLoader(),
                    geOnLoadResult(),
                    if (param == null) Dispatchers.Unconfined else getFetchDispatcher(param),
                    uiReceiver::onRefreshComplete
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
    private fun geOnCacheResult(): OnSourceCacheResult<Param> = ::onCacheResult

    private fun getLoader(): SourceLoader<Param> = ::load
    private fun geOnLoadResult(): OnSourceLoadResult<Param> = ::onLoadResult

    abstract fun getRefreshStrategy(): RefreshStrategy
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
    private val onCacheResult: OnSourceCacheResult<Param>,
    private val loader: SourceLoader<Param>,
    private val onLoadResult: OnSourceLoadResult<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
    private val onRefreshComplete: Invoke
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

        repoEventCh.send(RepoEvent.Loading())

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
        onCacheResult(cacheParams, cacheResult)

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
                val event = RepoEvent.Success(sources) {
                    onRefreshComplete()
                }
                repoEventCh.send(event)
            }
            is ItemSourceRepository.LoadResult.Error -> {
                val event = RepoEvent.Error(loadResult.error, usingCache) {
                    onRefreshComplete()
                }
                repoEventCh.send(event)
            }
        }
        onLoadResult(loadParams, loadResult)
    }

    fun close() {
        repoEventChannelFlowJob.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    private fun newSources(sources: List<ItemSourceRepository.ItemSourceInfo>): List<LazySourceData<Any>> {
        return sources
            .mapIndexed { index, itemSourceInfo ->
                val sourceToken: Any = itemSourceInfo.sourceToken
                val newItemSource = itemSourceInfo.source as ItemSource<Any>
                newItemSource.delegate.initPosition(index)
                val lazyFlow: Deferred<Flow<SourceData>> = GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
                    val itemFetcher = ItemFetcher(newItemSource)
                    newItemSource.delegate.injectRefreshActuator(itemFetcher::refresh)
                    itemFetcher.flow
                }
                LazySourceData(sourceToken, newItemSource, lazyFlow) { oldItemSource ->
                    oldItemSource.delegate.updateItemSource(index, newItemSource)
                }
            }
    }
}

internal typealias SourceCacheLoader<Param> = (suspend (ItemSourceRepository.CacheParams<Param>) -> ItemSourceRepository.CacheResult)
internal typealias OnSourceCacheResult<Param> = (suspend (ItemSourceRepository.CacheParams<Param>, ItemSourceRepository.CacheResult) -> Unit)
internal typealias SourceLoader<Param> = (suspend (ItemSourceRepository.LoadParams<Param>) -> ItemSourceRepository.LoadResult)
internal typealias OnSourceLoadResult<Param> = (suspend (ItemSourceRepository.LoadParams<Param>, ItemSourceRepository.LoadResult) -> Unit)