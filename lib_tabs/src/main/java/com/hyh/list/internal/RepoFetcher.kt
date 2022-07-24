package com.hyh.list.internal

import android.util.Log
import androidx.lifecycle.Lifecycle
import com.hyh.Invoke
import com.hyh.base.RefreshEventHandler
import com.hyh.base.RefreshStrategy
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.lifecycle.ChildLifecycleOwner
import com.hyh.lifecycle.IChildLifecycleOwner
import com.hyh.list.*
import com.hyh.list.internal.base.BaseItemFetcher
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.paging.PagingSourceItemFetcher
import com.hyh.list.internal.utils.IElementDiff
import com.hyh.list.internal.utils.ListUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlin.math.min


abstract class ItemSourceFetcher<Param>(private val initialParam: Param) : IFetcher<Param>,
    IChildLifecycleOwner {

    companion object {
        private const val TAG = "ItemSourceFetcher"
    }

    override val lifecycleOwner: ChildLifecycleOwner = ChildLifecycleOwner()

    private val repoDisplayedData = RepoDisplayedData()

    private val uiReceiver = object : UiReceiverForRepo<Param> {

        private val refreshEventHandler = object : RefreshEventHandler<Param>(initialParam) {

            override fun getRefreshStrategy(): RefreshStrategy {
                return this@ItemSourceFetcher.getRefreshStrategy()
            }
        }

        val flow = refreshEventHandler.flow

        override fun attach(lifecycle: Lifecycle) {
            Log.i(TAG, "uiReceiver:$this attach")
            bindParentLifecycle(lifecycle)
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
        }

        override fun refresh(param: Param) {
            Log.i(TAG, "uiReceiver:$this refresh:$param")
            refreshEventHandler.onReceiveRefreshEvent(false, param)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onRefreshComplete()
        }

        override fun detach() {
            Log.i(TAG, "uiReceiver:$this detach")
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED
            //refreshEventHandler.onDestroy()
            this@ItemSourceFetcher.destroy()
        }
    }

    val flow: Flow<RepoData<Param>> = simpleChannelFlow<RepoData<Param>> {
        uiReceiver
            .flow
            .simpleScan(null) { previousSnapshot: ItemSourceFetcherSnapshot<Param>?, param: Param ->
                previousSnapshot?.close()
                ItemSourceFetcherSnapshot(
                    lifecycleOwner.lifecycle,
                    param,
                    repoDisplayedData,
                    getCacheLoader(),
                    getLoader(),
                    if (param == null) Dispatchers.Unconfined else getFetchDispatcher(
                        param,
                        repoDisplayedData
                    ),
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

    private fun getCacheLoader(): SourceCacheLoader<Param> = ::getCache
    private fun getLoader(): SourceLoader<Param> = ::load

    abstract fun getRefreshStrategy(): RefreshStrategy
    abstract suspend fun getCache(params: ItemSourceRepo.CacheParams<Param>): ItemSourceRepo.CacheResult

    abstract suspend fun load(params: ItemSourceRepo.LoadParams<Param>): ItemSourceRepo.LoadResult

    abstract fun getFetchDispatcher(
        param: Param,
        displayedData: RepoDisplayedData
    ): CoroutineDispatcher

    private fun destroy() {
        repoDisplayedData.sources?.forEach {
            it.delegate.detach()
        }
        repoDisplayedData.clear()
    }

    // region IFetcher
    override fun refreshRepo(param: Param) {
        uiReceiver.refresh(param)
    }

    override fun refreshSources(important: Boolean) {
        repoDisplayedData.sources?.forEach {
            it.refreshActuator(important)
        }
    }

    override fun refreshSources(vararg sourceIndexes: Int, important: Boolean) {
        var index = 0
        val sourceIndexList = sourceIndexes.toMutableList()
        repoDisplayedData.sources?.forEach {
            if (sourceIndexList.isEmpty()) return@refreshSources
            if (sourceIndexList.remove(index)) {
                it.refreshActuator(important)
            }
            index++
        }
    }

    override fun refreshSources(sourceIndexStart: Int, count: Int, important: Boolean) {
        val sources = repoDisplayedData.sources
        val size = sources?.size ?: return
        if (sourceIndexStart >= size) {
            return
        }
        val end = min(sourceIndexStart + count - 1, size - 1)
        for (index in sourceIndexStart..end) {
            sources[index].refreshActuator(important)
        }
    }

    override fun refreshSources(vararg sourceTokens: Any, important: Boolean) {
        val sourcesMap = repoDisplayedData.sourcesMap ?: return
        sourceTokens.forEach {
            sourcesMap[it]?.refreshActuator?.invoke(important)
        }
    }

    override fun refreshSources(sourceTokenStart: Any, count: Int, important: Boolean) {
        val baseItemSource = repoDisplayedData.sourcesMap?.get(sourceTokenStart) ?: return
        val sourcePosition = baseItemSource.sourcePosition
        refreshSources(sourcePosition, count, important)
    }

    override fun sourceAppend(sourceToken: Any, important: Boolean) {
        val baseItemSource = repoDisplayedData.sourcesMap?.get(sourceToken) ?: return
        if (baseItemSource is ItemPagingSource<*, *>) {
            baseItemSource.appendActuator(important)
        }
    }

    override fun sourceRearrange(sourceToken: Any, important: Boolean) {
        val baseItemSource = repoDisplayedData.sourcesMap?.get(sourceToken) ?: return
        if (baseItemSource is ItemPagingSource<*, *>) {
            baseItemSource.rearrangeActuator(important)
        }
    }

    // endregion

}

class RepoResultProcessorGenerator(
    private val repoLifecycle: Lifecycle,
    private val repoDisplayedData: RepoDisplayedData,
    private val sources: List<BaseItemSource<*, *>>,
    private val resultExtra: Any?
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
                val itemFetcher = createItemFetcher(itemSource)
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
            ItemSourceDiff()
        )

        val sourceIndexMap: MutableMap<Any, Int> = mutableMapOf()

        val lazySources = mutableListOf<LazySourceData>()
        val sources = mutableListOf<BaseItemSource<*, *>>()
        val sourcesMap = mutableMapOf<Any, BaseItemSource<*, *>>()

        updateResult.resultList.forEachIndexed { index, itemSourceWrapper ->
            lazySources.add(itemSourceWrapper.lazySourceData)
            sources.add(itemSourceWrapper.itemSource)
            sourcesMap[itemSourceWrapper.sourceToken] = itemSourceWrapper.itemSource
            sourceIndexMap[itemSourceWrapper.sourceToken] = index
        }

        return RepoProcessedResult(lazySources, updateResult.listOperates) {
            repoDisplayedData.lazySources = lazySources
            repoDisplayedData.sources = sources
            repoDisplayedData.sourcesMap = sourcesMap
            repoDisplayedData.resultExtra = resultExtra

            lazySources.forEach {
                it.lazyFlow.value
            }

            updateResult.elementOperates.removedElements.forEach {
                it.itemSource.delegate.detach()
            }
            updateResult.elementOperates.addedElements.forEach {
                it.itemSource.delegate.attach()
            }
            updateResult.elementOperates.changedElements.forEach {
                val oldWrapper = it.first
                val newWrapper = it.second
                oldWrapper.itemSource.delegate.sourcePosition =
                    sourceIndexMap[oldWrapper.sourceToken] ?: -1

                @Suppress("UNCHECKED_CAST")
                (oldWrapper.itemSource.delegate as BaseItemSource.Delegate<Any?, Any?>)
                    .updateItemSource((newWrapper.itemSource as BaseItemSource<Any?, Any?>))
            }
        }
    }

    private fun createItemFetcher(itemSource: BaseItemSource<*, *>): BaseItemFetcher<*, *> {
        return when (itemSource) {
            is ItemPagingSource<*, *> -> PagingSourceItemFetcher(itemSource).apply {
                itemSource.delegate.bindParentLifecycle(repoLifecycle)
                itemSource.delegate.injectRefreshActuator(::refresh)
                itemSource.delegate.injectItemOperator(this.itemOperator)
                itemSource.delegate.injectAppendActuator(::append)
                itemSource.delegate.injectRearrangeActuator(::rearrange)
            }
            else -> ItemFetcher(itemSource as ItemSource<*, *>).apply {
                itemSource.delegate.bindParentLifecycle(repoLifecycle)
                itemSource.delegate.injectRefreshActuator(::refresh)
                itemSource.delegate.injectItemOperator(this.itemOperator)
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
        val itemSource: BaseItemSource<*, *>,
        val lazySourceData: LazySourceData
    ) {
        override fun equals(other: Any?): Boolean {
            return sourceToken == (other as? ItemSourceWrapper)?.sourceToken
        }

        override fun hashCode(): Int {
            return sourceToken.hashCode()
        }
    }

    private class ItemSourceDiff : IElementDiff<ItemSourceWrapper> {

        override fun isSupportUpdate(
            oldElement: ItemSourceWrapper,
            newElement: ItemSourceWrapper
        ): Boolean = true

        override fun areItemsTheSame(
            oldElement: ItemSourceWrapper,
            newElement: ItemSourceWrapper
        ): Boolean {
            return oldElement.sourceToken == newElement.sourceToken
        }

        @Suppress("UNCHECKED_CAST")
        override fun areContentsTheSame(
            oldElement: ItemSourceWrapper,
            newElement: ItemSourceWrapper
        ): Boolean {
            val delegate = oldElement.itemSource.delegate as BaseItemSource.Delegate<Any, Any>
            return delegate.areSourceContentsTheSame(newElement.itemSource as BaseItemSource<Any, Any>)
        }

        override fun getChangePayload(
            oldElement: ItemSourceWrapper,
            newElement: ItemSourceWrapper
        ): Any? {
            return null
        }
    }
}

class ItemSourceFetcherSnapshot<Param>(
    private val repoLifecycle: Lifecycle,
    private val param: Param,
    private val displayedData: RepoDisplayedData,
    private val cacheLoader: SourceCacheLoader<Param>,
    private val loader: SourceLoader<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
    private val onRefreshComplete: Invoke
) {

    @Volatile
    private var closed = false
    private val repoEventChannelFlowJob = Job()
    private val repoEventCh = Channel<RepoEvent>(Channel.BUFFERED)

    val repoEventFlow: Flow<RepoEvent> = cancelableChannelFlow(repoEventChannelFlowJob) {
        launch {
            repoEventCh.consumeAsFlow().collect {
                try {
                    if (closed) return@collect
                    send(it)
                } catch (e: ClosedSendChannelException) {
                }
            }
        }

        if (param == null) {
            return@cancelableChannelFlow
        }

        repoEventCh.send(RepoEvent.Loading())

        val cacheParams = ItemSourceRepo.CacheParams<Param>(param, displayedData)
        val cacheResult = cacheLoader.invoke(cacheParams)
        var usingCache = false
        if (cacheResult is ItemSourceRepo.CacheResult.Success) {
            usingCache = true
            val event = RepoEvent.UsingCache(
                RepoResultProcessorGenerator(
                    repoLifecycle,
                    displayedData,
                    cacheResult.sources,
                    cacheResult.resultExtra
                ).processor
            )
            repoEventCh.send(event)
        }

        val loadParams = ItemSourceRepo.LoadParams<Param>(param, displayedData)
        val loadResult: ItemSourceRepo.LoadResult = if (fetchDispatcher == null) {
            loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loader.invoke(loadParams)
            }
        }
        when (loadResult) {
            is ItemSourceRepo.LoadResult.Success -> {
                val event = RepoEvent.Success(
                    RepoResultProcessorGenerator(
                        repoLifecycle,
                        displayedData,
                        loadResult.sources,
                        loadResult.resultExtra
                    ).processor
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
        closed = true
        repoEventChannelFlowJob.cancel()
    }
}

internal typealias SourceCacheLoader<Param> = (suspend (ItemSourceRepo.CacheParams<Param>) -> ItemSourceRepo.CacheResult)
internal typealias SourceLoader<Param> = (suspend (ItemSourceRepo.LoadParams<Param>) -> ItemSourceRepo.LoadResult)