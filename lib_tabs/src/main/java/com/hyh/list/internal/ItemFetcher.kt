package com.hyh.list.internal

import android.os.SystemClock
import com.hyh.Invoke
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.ItemData
import com.hyh.list.ItemSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlin.math.abs


class ItemFetcher<Param : Any>(
    private val itemSource: ItemSource<Param>,
) {

    private val uiReceiver = object : UiReceiverForSource {

        private val state = MutableStateFlow<Long>(0)

        private var cacheState: MutableStateFlow<Long>? = null
        private var refreshStage = RefreshStage.UNBLOCK
        private var delay = 0
        private var timingStart: Long = 0

        val flow = state.asStateFlow()

        override fun refresh() {
            when (refreshStage) {
                RefreshStage.UNBLOCK -> {
                    state.value = state.value + 1
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
                        }
                    }
                }
                RefreshStage.TIMING -> {
                    state.value = state.value + 1
                    val elapsedRealtime = SystemClock.elapsedRealtime()
                    if (abs(elapsedRealtime - timingStart) > delay) {
                        refreshStage = RefreshStage.BLOCK
                    }
                }
                RefreshStage.BLOCK -> {
                    val cacheState = this.cacheState
                    if (cacheState != null) {
                        cacheState.value = cacheState.value + 1
                    } else {
                        this.cacheState = MutableStateFlow(0)
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
                refresh()
            }
        }
    }

    val flow: Flow<SourceData> = simpleChannelFlow<SourceData> {
        uiReceiver
            .flow
            .onStart {
                emit(0)
            }
            .flowOn(Dispatchers.Main)
            .simpleScan(null) { previousSnapshot: ItemFetcherSnapshot<Param>?, _: Long ->
                previousSnapshot?.close()
                ItemFetcherSnapshot(
                    lastPreShowResult = previousSnapshot?.preShowResult,
                    lastLoadResult = previousSnapshot?.loadResult,
                    paramProvider = getParamProvider(),
                    preShowLoader = getPreShowLoader(),
                    onPreShowResult = getOnPreShowResult(),
                    loader = getLoader(),
                    onItemLoadResult = getOnLoadResult(),
                    fetchDispatcherProvider = getFetchDispatcherProvider(),
                    lastDisplayedItemsBucketIds = previousSnapshot?.displayedItemsBucketIds,
                    lastDisplayedItemsBucketMap = previousSnapshot?.displayedItemsBucketMap,
                    lastDisplayedItemWrappers = previousSnapshot?.displayedItemWrappers,
                    lastDisplayedItems = previousSnapshot?.displayedItems,
                    onRefreshComplete = uiReceiver::onRefreshComplete
                )
            }
            .filterNotNull()
            .simpleMapLatest { snapshot ->
                val downstreamFlow = snapshot.sourceEventFlow
                SourceData(downstreamFlow, uiReceiver)
            }
            .collect {
                send(it)
            }
    }.buffer(Channel.BUFFERED)

    fun refresh() {
        uiReceiver.refresh()
    }

    private fun getRefreshStrategy(): RefreshStrategy {
        return itemSource.getRefreshStrategy()
    }

    private fun getParamProvider(): ParamProvider<Param> = ::getParam
    private fun getFetchDispatcherProvider(): FetchDispatcherProvider<Param> = ::getFetchDispatcher

    private fun getPreShowLoader(): PreShowLoader<Param> = ::getPreShow
    private fun getOnPreShowResult(): OnPreShowResult<Param> = ::onPreShowResult

    private fun getLoader(): ItemLoader<Param> = ::load
    private fun getOnLoadResult(): OnItemLoadResult<Param> = ::onLoadResult

    private suspend fun getParam(): Param {
        return itemSource.getParam()
    }

    private fun getFetchDispatcher(param: Param): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param)
    }

    private suspend fun getPreShow(params: ItemSource.PreShowParams<Param>): ItemSource.PreShowResult {
        return itemSource.getPreShow(params)
    }

    private suspend fun onPreShowResult(params: ItemSource.PreShowParams<Param>, preShowResult: ItemSource.PreShowResult) {
        itemSource.onPreShowResult(params, preShowResult)
    }

    private suspend fun load(params: ItemSource.LoadParams<Param>): ItemSource.LoadResult {
        return itemSource.load(params)
    }

    private suspend fun onLoadResult(params: ItemSource.LoadParams<Param>, loadResult: ItemSource.LoadResult) {
        itemSource.onLoadResult(params, loadResult)
    }

}


class ItemFetcherSnapshot<Param : Any>(
    private val lastPreShowResult: ItemSource.PreShowResult? = null,
    private val lastLoadResult: ItemSource.LoadResult? = null,
    private val paramProvider: ParamProvider<Param>,
    private val preShowLoader: PreShowLoader<Param>,
    private val onPreShowResult: OnPreShowResult<Param>,
    private val loader: ItemLoader<Param>,
    private val onItemLoadResult: OnItemLoadResult<Param>,
    private val fetchDispatcherProvider: FetchDispatcherProvider<Param>,
    private val lastDisplayedItemsBucketIds: List<Int>?,
    private val lastDisplayedItemsBucketMap: Map<Int, ItemSource.ItemsBucket>?,
    private val lastDisplayedItemWrappers: List<ItemDataWrapper>?,
    private val lastDisplayedItems: List<ItemData>?,
    private val onRefreshComplete: Invoke
) {


    var displayedItemsBucketIds: List<Int>? = lastDisplayedItemsBucketIds
    var displayedItemsBucketMap: Map<Int, ItemSource.ItemsBucket>? = lastDisplayedItemsBucketMap
    var displayedItemWrappers: List<ItemDataWrapper>? = lastDisplayedItemWrappers
    var displayedItems: List<ItemData>? = lastDisplayedItems

    var preShowResult: ItemSource.PreShowResult? = null
    var loadResult: ItemSource.LoadResult? = null

    private val sourceEventChannelFlowJob = Job()
    private val sourceEventCh = Channel<SourceEvent>(Channel.BUFFERED)

    val sourceEventFlow: Flow<SourceEvent> = cancelableChannelFlow(sourceEventChannelFlowJob) {
        launch {
            sourceEventCh.consumeAsFlow().collect {
                // Protect against races where a subsequent call to submitData invoked close(),
                // but a tabEvent arrives after closing causing ClosedSendChannelException.
                try {
                    send(it)
                } catch (e: ClosedSendChannelException) {
                    // Safe to drop tabEvent here, since collection has been cancelled.
                }
            }
        }

        sourceEventCh.send(SourceEvent.Loading())


        val param = paramProvider.invoke()
        val fetchDispatcher = fetchDispatcherProvider.invoke(param)

        val preShowParams = ItemSource.PreShowParams(param, getDisplayedItemsSnapshot(), lastPreShowResult, lastLoadResult)
        val preShowResult = preShowLoader.invoke(preShowParams)
        this@ItemFetcherSnapshot.preShowResult = preShowResult

        var preShowing = false
        if (preShowResult is ItemSource.PreShowResult.Success) {
            preShowing = true
            val itemsBucketIds = preShowResult.itemsBucketIds
            val itemsBucketMap = preShowResult.itemsBucketMap
            val wrappers = getItemWrappers(itemsBucketIds, itemsBucketMap)
            val updateResult =
                ListUpdate.calculateDiff(
                    lastDisplayedItemWrappers,
                    wrappers,
                    IElementDiff.ItemDataWrapperDiff()
                )

            //val itemsBucketMap = mutableMapOf<Int, ItemSource.ItemsBucket>()
            updateResult.oldElementInResultList.forEach {
                itemsBucketMap[it.itemsBucketId]
            }

            //lastDisplayedItemsBucketIds
            //新增的与删除的ID
            //Token发生变化的ID


            val items = updateResult.resultList.map { it.itemData }
            val event = SourceEvent.PreShowing(items, updateResult.listOperates) {
                onSuccessEventReceived(itemsBucketIds, itemsBucketMap, updateResult, items)
            }
            sourceEventCh.send(event)
        }
        onPreShowResult(preShowParams, preShowResult)

        val loadParams = ItemSource.LoadParams(param, getDisplayedItemsSnapshot(), lastPreShowResult, lastLoadResult)
        val loadResult: ItemSource.LoadResult
        loadResult = if (fetchDispatcher == null) {
            loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loader.invoke(loadParams)
            }
        }
        this@ItemFetcherSnapshot.loadResult = loadResult

        when (loadResult) {
            is ItemSource.LoadResult.Success -> {
                val itemsBucketIds = loadResult.itemsBucketIds
                val itemsBucketMap = loadResult.itemsBucketMap
                val wrappers = getItemWrappers(itemsBucketIds, itemsBucketMap)
                val updateResult =
                    ListUpdate.calculateDiff(
                        lastDisplayedItemWrappers,
                        wrappers,
                        IElementDiff.ItemDataWrapperDiff()
                    )


                val resultItemsBucketMap: MutableMap<Int, ItemSource.ItemsBucket> = mutableMapOf()
                val resultItems = mutableListOf<ItemData>()
                updateResult.resultList.forEach {
                    var itemsBucket = resultItemsBucketMap[it.itemsBucketId]
                    if (itemsBucket == null) {
                        val items = mutableListOf<ItemData>()
                        items.add(it.itemData)
                        itemsBucket = ItemSource.ItemsBucket(it.itemsBucketId, it.itemsToken, items)
                        resultItemsBucketMap[it.itemsBucketId] = itemsBucket
                    } else {
                        (itemsBucket.items as MutableList<ItemData>).add(it.itemData)
                    }
                    resultItems.add(it.itemData)
                }


                val items = updateResult.resultList.map { it.itemData }
                val event = SourceEvent.Success(items, updateResult.listOperates) {
                    onSuccessEventReceived(itemsBucketIds, itemsBucketMap, updateResult, items)
                    onRefreshComplete()
                }
                sourceEventCh.send(event)
            }
            is ItemSource.LoadResult.Error -> {
                val event = SourceEvent.Error(loadResult.error, preShowing) {
                    onRefreshComplete()
                }
                sourceEventCh.send(event)
            }
        }
        onItemLoadResult(loadParams, loadResult)
    }

    fun close() {
        sourceEventChannelFlowJob.cancel()
    }

    private fun getDisplayedItemsSnapshot(): List<ItemData>? {
        val displayItems = this.lastDisplayedItems ?: return null
        return ArrayList(displayItems)
    }

    private fun getItemWrappers(
        itemsBucketIds: List<Int>,
        itemsBucketMap: Map<Int, ItemSource.ItemsBucket>
    ): List<ItemDataWrapper> {
        val wrappers = mutableListOf<ItemDataWrapper>()
        itemsBucketIds.forEach { id ->
            val itemsBucket = itemsBucketMap[id]
            if (itemsBucket != null) {
                wrappers.addAll(
                    itemsBucket.items.map { ItemDataWrapper(id, itemsBucket.itemsToken, it) }
                )
            }
        }
        return wrappers
    }

    private fun onSuccessEventReceived(
        itemsBucketIds: List<Int>,
        itemsBucketMap: Map<Int, ItemSource.ItemsBucket>,
        updateResult: ListUpdate.UpdateResult<ItemDataWrapper>,
        items: List<ItemData>
    ) {
        displayedItemsBucketIds = itemsBucketIds
        displayedItemsBucketMap = itemsBucketMap
        displayedItemWrappers = updateResult.resultList
        displayedItems = items
        updateResult.resultList.forEach {
            it.itemData.delegate.displayedItems = displayedItems
        }
        ListUpdate.handleItemDataWrapperChanges(updateResult.elementOperates)
    }
}

internal typealias ParamProvider<Param> = (suspend () -> Param)
internal typealias PreShowLoader<Param> = (suspend (params: ItemSource.PreShowParams<Param>) -> ItemSource.PreShowResult)
internal typealias OnPreShowResult<Param> = (suspend (ItemSource.PreShowParams<Param>, ItemSource.PreShowResult) -> Unit)
internal typealias ItemLoader<Param> = (suspend (param: ItemSource.LoadParams<Param>) -> ItemSource.LoadResult)
internal typealias OnItemLoadResult<Param> = (suspend (ItemSource.LoadParams<Param>, ItemSource.LoadResult) -> Unit)
internal typealias FetchDispatcherProvider<Param> = ((Param) -> CoroutineDispatcher?)