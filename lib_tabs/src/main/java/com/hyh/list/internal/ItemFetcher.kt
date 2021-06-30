package com.hyh.list.internal

import android.util.Log
import com.hyh.*
import com.hyh.base.BaseLoadEventHandler
import com.hyh.base.LoadStrategy
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
import kotlin.collections.LinkedHashMap


class ItemFetcher<Param : Any>(
    private val itemSource: ItemSource<Param>,
) {


    private val sourceDelegateRunner: RunWith<ItemSource.Delegate<Param>> = {
        it.invoke(itemSource.delegate)
    }

    private val uiReceiver = object : UiReceiverForSource {

        private val refreshEventHandler = object : BaseLoadEventHandler<Unit>(Unit) {

            override fun getLoadStrategy(): LoadStrategy {
                return this@ItemFetcher.getLoadStrategy()
            }
        }

        val flow = refreshEventHandler.flow.map { it.first }

        override fun refresh(important: Boolean) {
            refreshEventHandler.onReceiveLoadEvent(important, Unit)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onLoadComplete()
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
                    onRefreshComplete = uiReceiver::onRefreshComplete,
                    sourceDelegateRunner = sourceDelegateRunner
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

    fun refresh(important: Boolean) {
        uiReceiver.refresh(important)
    }

    private fun getLoadStrategy(): LoadStrategy {
        return itemSource.getLoadStrategy()
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
    lastDisplayedItemsBucketIds: List<Int>?,
    lastDisplayedItemsBucketMap: Map<Int, ItemSource.ItemsBucket>?,
    lastDisplayedItemWrappers: List<ItemDataWrapper>?,
    lastDisplayedItems: List<ItemData>?,
    private val onRefreshComplete: Invoke,
    private val sourceDelegateRunner: RunWith<ItemSource.Delegate<Param>>
) {

    companion object {
        private const val TAG = "ItemFetcher"
    }

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
                    Log.d(TAG, "send : $it")
                } catch (e: ClosedSendChannelException) {
                    // Safe to drop tabEvent here, since collection has been cancelled.
                }
            }
        }

        sourceEventCh.send(SourceEvent.Loading())

        val param = paramProvider.invoke()


        Log.d(TAG, "handlePreShowStep: start")
        val start = System.currentTimeMillis()

        handlePreShowStep(param) {
            val end = System.currentTimeMillis()
            Log.d(TAG, "handlePreShowStep: use time -> ${end - start}")
            handleLoadStep(param, it)
        }
    }

    private suspend fun handleLoadStep(param: Param, preShowing: Boolean) {
        val fetchDispatcher = fetchDispatcherProvider.invoke(param)

        val loadParams = ItemSource.LoadParams(param, displayedItemsBucketMap, displayedItems, lastPreShowResult, lastLoadResult)
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

                val processedResult = processResult(itemsBucketIds, itemsBucketMap)

                val event = SourceEvent.Success(processedResult.resultItems, processedResult.listOperates) {
                    onSuccessEventReceived(itemsBucketIds, processedResult)
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

    private suspend fun ItemFetcherSnapshot<Param>.handlePreShowStep(
        param: Param,
        nextStepInvoke: (suspend (preShowing: Boolean) -> Unit)
    ): Boolean {
        val preShowParams = ItemSource.PreShowParams(param, displayedItemsBucketMap, displayedItems, lastPreShowResult, lastLoadResult)
        val preShowResult = preShowLoader.invoke(preShowParams)
        this@ItemFetcherSnapshot.preShowResult = preShowResult

        var preShowing = false
        if (preShowResult is ItemSource.PreShowResult.Success) {
            preShowing = true
            val itemsBucketIds = preShowResult.itemsBucketIds
            val itemsBucketMap = preShowResult.itemsBucketMap

            val processedResult = processResult(itemsBucketIds, itemsBucketMap)

            val onEventReceived: OnEventReceived = {
                onSuccessEventReceived(
                    itemsBucketIds,
                    processedResult
                )

                nextStepInvoke(true)
            }

            val event = SourceEvent.PreShowing(
                processedResult.resultItems,
                processedResult.listOperates,
                onEventReceived
            )

            sourceEventCh.send(event)

            onPreShowResult(preShowParams, preShowResult)
        } else {
            onPreShowResult(preShowParams, preShowResult)

            nextStepInvoke(false)
        }
        return preShowing
    }

    fun close() {
        sourceEventChannelFlowJob.cancel()
    }

    private fun processResult(
        itemsBucketIds: List<Int>,
        itemsBucketMap: Map<Int, ItemSource.ItemsBucket>
    ): XXProcessedResult<Param> {
        val wrappers = getItemWrappers(itemsBucketIds, itemsBucketMap)
        val updateResult =
            ListUpdate.calculateDiff(
                displayedItemWrappers,
                wrappers,
                IElementDiff.ItemDataWrapperDiff()
            )


        val resultItemsBucketMap: MutableMap<Int, ItemSource.ItemsBucket> = LinkedHashMap()
        itemsBucketIds.forEach {
            val items = mutableListOf<ItemData>()
            resultItemsBucketMap[it] = ItemSource.ItemsBucket(it, ItemSource.DEFAULT_ITEMS_TOKEN, items)
        }

        val resultItemWrappers = mutableListOf<ItemDataWrapper>()
        val resultItems = mutableListOf<ItemData>()


        updateResult.resultList.forEachIndexed { index, wrapper ->
            val newWrapper = wrappers[index]
            newWrapper.itemData = wrapper.itemData

            var itemsBucket = resultItemsBucketMap[newWrapper.itemsBucketId]
            if (itemsBucket == null || itemsBucket.itemsToken != newWrapper.itemsToken) {
                val items = mutableListOf<ItemData>()
                items.add(newWrapper.itemData)

                itemsBucket = ItemSource.ItemsBucket(newWrapper.itemsBucketId, newWrapper.itemsToken, items)
                resultItemsBucketMap[newWrapper.itemsBucketId] = itemsBucket
            } else {
                (itemsBucket.items as MutableList<ItemData>).add(newWrapper.itemData)
            }

            resultItemWrappers.add(newWrapper)
            resultItems.add(newWrapper.itemData)
        }

        val oldItemsBuckets = displayedItemsBucketMap?.values?.toList() ?: emptyList()
        val newItemsBuckets = resultItemsBucketMap.values.toList()

        val itemsBucketsResult = ListUpdate.calculateDiff(
            oldItemsBuckets,
            newItemsBuckets,
            IElementDiff.BucketDiff()
        )

        val itemSourceInvoke: MutableList<InvokeWithParam<ItemSource.Delegate<Param>>> = mutableListOf()

        itemsBucketsResult.elementOperates.forEach { operate ->
            when (operate) {
                is ElementOperate.Added<ItemSource.ItemsBucket> -> {
                    itemSourceInvoke.add {
                        this.onBucketAdded(operate.element)
                    }
                }
                is ElementOperate.Changed<ItemSource.ItemsBucket> -> {
                    itemSourceInvoke.add {
                        if (this.shouldCacheBucket(operate.oldElement)) {
                            storage.take(
                                operate.oldElement.bucketId,
                                operate.oldElement.itemsToken
                            )?.items?.forEach {
                                it.delegate.cached = false
                            }

                            storage.store(operate.oldElement)

                            operate.oldElement.items.forEach {
                                it.delegate.cached = true
                            }
                        }
                    }
                }
                is ElementOperate.Removed<ItemSource.ItemsBucket> -> {
                    itemSourceInvoke.add {
                        this.onBucketRemoved(operate.element)
                    }
                }
            }
        }

        return XXProcessedResult(
            resultItemWrappers,
            updateResult.listOperates,
            updateResult.elementOperates,
            resultItemsBucketMap,
            resultItems,
            itemSourceInvoke
        )
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
        processedResult: XXProcessedResult<Param>
    ) {
        displayedItemsBucketIds = itemsBucketIds
        displayedItemsBucketMap = processedResult.resultItemsBucketMap
        displayedItemWrappers = processedResult.resultItemWrappers
        displayedItems = processedResult.resultItems
        processedResult.resultItemWrappers.forEach {
            it.itemData.delegate.displayedItems = displayedItems
        }

        sourceDelegateRunner {
            processedResult.itemSourceInvoke.forEach {
                it.invoke(this)
            }
        }

        ListUpdate.handleItemDataWrapperChanges(processedResult.elementOperates)
    }


}

internal typealias ParamProvider<Param> = (suspend () -> Param)
internal typealias PreShowLoader<Param> = (suspend (params: ItemSource.PreShowParams<Param>) -> ItemSource.PreShowResult)
internal typealias OnPreShowResult<Param> = (suspend (ItemSource.PreShowParams<Param>, ItemSource.PreShowResult) -> Unit)
internal typealias ItemLoader<Param> = (suspend (param: ItemSource.LoadParams<Param>) -> ItemSource.LoadResult)
internal typealias OnItemLoadResult<Param> = (suspend (ItemSource.LoadParams<Param>, ItemSource.LoadResult) -> Unit)
internal typealias FetchDispatcherProvider<Param> = ((Param) -> CoroutineDispatcher?)