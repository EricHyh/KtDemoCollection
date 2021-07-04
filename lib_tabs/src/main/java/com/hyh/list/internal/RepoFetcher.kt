package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.base.BaseLoadEventHandler
import com.hyh.base.LoadStrategy
import com.hyh.coroutine.*
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

    private val repoDisplayedData = RepoDisplayedData()

    private val uiReceiver = object : UiReceiverForRepo<Param> {

        private val refreshEventHandler = object : BaseLoadEventHandler<Param>(initialParam) {

            override fun getLoadStrategy(): LoadStrategy {
                return this@ItemSourceFetcher.getLoadStrategy()
            }
        }

        val flow = refreshEventHandler.flow.map { it.second }

        override fun refresh(param: Param) {
            refreshEventHandler.onReceiveLoadEvent(false, param)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onLoadComplete()
        }

        override fun close() {
            destroy()
        }
    }

    private val coroutineScope = CloseableCoroutineScope(SupervisorJob() + Dispatchers.Default)

    val flow: Flow<RepoData<Param>> = simpleChannelFlow<RepoData<Param>> {
        withContext(coroutineScope.coroutineContext) {
            uiReceiver
                .flow
                .simpleScan(null) { previousSnapshot: ItemSourceFetcherSnapshot<Param>?, param: Param? ->
                    previousSnapshot?.close()
                    ItemSourceFetcherSnapshot(
                        param,
                        repoDisplayedData,
                        getCacheLoader(),
                        getLoader(),
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
        }
    }.buffer(Channel.BUFFERED)

    private fun getCacheLoader(): SourceCacheLoader<Param> = ::getCache
    private fun getLoader(): SourceLoader<Param> = ::load

    abstract fun getLoadStrategy(): LoadStrategy
    abstract suspend fun getCache(params: ItemSourceRepo.CacheParams<Param>): ItemSourceRepo.CacheResult

    abstract suspend fun load(params: ItemSourceRepo.LoadParams<Param>): ItemSourceRepo.LoadResult

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher

    private fun destroy() {
        coroutineScope.cancel()
    }
}

class RepoResultProcessorGenerator(
    private val repoDisplayedData: RepoDisplayedData,
    private val sources: List<ItemSource<out Any, out Any>>,
    private val resultExtra: Any?,
) {

    val processor: RepoResultProcessor = {
        processResult()
    }

    private fun processResult(): RepoProcessedResult {
        val indexMap = mutableMapOf<Any, Int>()
        val lazySources = sources.mapIndexed { index, itemSource ->
            indexMap[itemSource.sourceToken] = index
            val lazyFlow: Lazy<Flow<SourceData>> = lazy {
                itemSource.delegate.sourcePosition = index
                val itemFetcher = ItemFetcher(itemSource)
                itemSource.delegate.injectRefreshActuator(itemFetcher::refresh)
                itemFetcher.flow
            }
            LazySourceData(itemSource, lazyFlow)
        }

        val updateResult = ListUpdate.calculateDiff(
            repoDisplayedData.lazySources,
            lazySources,
            IElementDiff.ItemSourceDiff()
        )

        return RepoProcessedResult(updateResult.resultList, updateResult.listOperates) {
            repoDisplayedData.lazySources = updateResult.resultList
            repoDisplayedData.resultExtra = resultExtra
        }
    }
}


class ItemSourceFetcherSnapshot<Param : Any>(
    private val param: Param?,
    private val displayedData: RepoDisplayedData,
    private val cacheLoader: SourceCacheLoader<Param>,
    private val loader: SourceLoader<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
    private val onRefreshComplete: Invoke
) {


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

        val cacheParams = ItemSourceRepo.CacheParams(param, displayedData)
        val cacheResult = cacheLoader.invoke(cacheParams)
        var usingCache = false
        if (cacheResult is ItemSourceRepo.CacheResult.Success) {
            usingCache = true
            val event = RepoEvent.UsingCache(
                RepoResultProcessorGenerator(displayedData, cacheResult.sources, cacheResult.resultExtra).processor
            )
            repoEventCh.send(event)
        }

        val loadParams = ItemSourceRepo.LoadParams(param, displayedData)
        val loadResult: ItemSourceRepo.LoadResult
        loadResult = if (fetchDispatcher == null) {
            loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loader.invoke(loadParams)
            }
        }
        when (loadResult) {
            is ItemSourceRepo.LoadResult.Success -> {
                val event = RepoEvent.Success(
                    RepoResultProcessorGenerator(displayedData, loadResult.sources, loadResult.resultExtra).processor
                ) {
                    onRefreshComplete()
                }
                repoEventCh.send(event)
            }
            is ItemSourceRepo.LoadResult.Error -> {
                val event = RepoEvent.Error(loadResult.error, usingCache) {
                    onRefreshComplete()
                }
                repoEventCh.send(event)
            }
        }
    }

    fun close() {
        repoEventChannelFlowJob.cancel()
    }
}

internal typealias SourceCacheLoader<Param> = (suspend (ItemSourceRepo.CacheParams<Param>) -> ItemSourceRepo.CacheResult)
internal typealias SourceLoader<Param> = (suspend (ItemSourceRepo.LoadParams<Param>) -> ItemSourceRepo.LoadResult)