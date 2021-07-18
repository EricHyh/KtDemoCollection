package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.base.RefreshEventHandler
import com.hyh.base.RefreshStrategy
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

        private val refreshEventHandler = object : RefreshEventHandler<Param>(initialParam) {

            override fun getRefreshStrategy(): RefreshStrategy {
                return this@ItemSourceFetcher.getRefreshStrategy()
            }
        }

        val flow = refreshEventHandler.flow.map { it.second }

        override fun refresh(param: Param) {
            refreshEventHandler.onReceiveRefreshEvent(false, param)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onRefreshComplete()
        }

        override fun destroy() {
            refreshEventHandler.onDestroy()
            this@ItemSourceFetcher.destroy()
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

    abstract fun getRefreshStrategy(): RefreshStrategy
    abstract suspend fun getCache(params: ItemSourceRepo.CacheParams<Param>): ItemSourceRepo.CacheResult

    abstract suspend fun load(params: ItemSourceRepo.LoadParams<Param>): ItemSourceRepo.LoadResult

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher

    private fun destroy() {
        coroutineScope.cancel()
        repoDisplayedData.sources?.forEach {
            it.delegate.detach()
        }
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
        val sourceWrappers = sources.mapIndexed { index, itemSource ->
            indexMap[itemSource.sourceToken] = index
            val lazyFlow: Lazy<Flow<SourceData>> = lazy {
                itemSource.delegate.sourcePosition = index
                val itemFetcher = ItemFetcher(itemSource)
                itemSource.delegate.injectRefreshActuator(itemFetcher::refresh)
                itemFetcher.flow
            }
            ItemSourceWrapper(
                itemSource.sourceToken,
                itemSource,
                LazySourceData(itemSource.sourceToken, lazyFlow)
            )
        }

        val updateResult = ListUpdate.calculateDiff(
            repoDisplayedData.getItemSourceWrappers(),
            sourceWrappers,
            IElementDiff.AnyDiff()
        )

        val sourceIndexMap: MutableMap<Any, Int> = mutableMapOf()

        val lazySources = mutableListOf<LazySourceData>()
        val sources = mutableListOf<ItemSource<out Any, out Any>>()
        updateResult.resultList.forEachIndexed { index, itemSourceWrapper ->
            lazySources.add(itemSourceWrapper.lazySourceData)
            sources.add(itemSourceWrapper.itemSource)
            sourceIndexMap[itemSourceWrapper.sourceToken] = index
        }

        return RepoProcessedResult(lazySources, updateResult.listOperates) {
            repoDisplayedData.lazySources = lazySources
            repoDisplayedData.sources = sources
            repoDisplayedData.resultExtra = resultExtra

            updateResult.elementOperates.removedElements.forEach {
                it.itemSource.delegate.detach()
            }
            updateResult.elementOperates.addedElements.forEach {
                it.itemSource.delegate.attach()
            }
            updateResult.elementOperates.changedElements.forEach {
                val oldWrapper = it.first
                val newWrapper = it.second
                oldWrapper.itemSource.delegate.sourcePosition = sourceIndexMap[oldWrapper.sourceToken] ?: -1
                @Suppress("UNCHECKED_CAST")
                (oldWrapper.itemSource.delegate as ItemSource.Delegate<Any, Any>)
                    .updateItemSource((newWrapper.itemSource as ItemSource<Any, Any>))
            }
        }
    }

    private fun RepoDisplayedData.getItemSourceWrappers(): List<ItemSourceWrapper> {
        val lazySources = this.lazySources
        val sources = this.sources
        if (lazySources == null || sources == null) return emptyList()
        return lazySources.mapIndexed { index, lazySourceData ->
            ItemSourceWrapper(
                lazySourceData.sourceToken,
                sources[index],
                lazySourceData
            )
        }
    }

    private class ItemSourceWrapper(
        val sourceToken: Any,
        val itemSource: ItemSource<out Any, out Any>,
        val lazySourceData: LazySourceData
    ) {
        override fun equals(other: Any?): Boolean {
            return sourceToken == (other as? ItemSourceWrapper)?.sourceToken
        }

        override fun hashCode(): Int {
            return sourceToken.hashCode()
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