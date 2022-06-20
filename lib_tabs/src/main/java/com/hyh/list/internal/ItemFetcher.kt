package com.hyh.list.internal

import android.util.Log
import com.hyh.*
import com.hyh.base.RefreshEventHandler
import com.hyh.base.RefreshStrategy
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.ItemSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*


class ItemFetcher<Param : Any, Item : Any>(
    private val itemSource: ItemSource<Param, Item>
) : BaseItemFetcher<Param, Item>(itemSource) {

    inner class ItemFetcherUiReceiver : BaseUiReceiverForSource() {

        private val refreshEventHandler = object : RefreshEventHandler<Unit>(Unit) {

            override fun getRefreshStrategy(): RefreshStrategy {
                return this@ItemFetcher.getRefreshStrategy()
            }
        }

        val flow = refreshEventHandler.flow

        override fun refresh(important: Boolean) {
            refreshEventHandler.onReceiveRefreshEvent(important, Unit)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onRefreshComplete()
        }

        override fun destroy() {
            super.destroy()
            refreshEventHandler.onDestroy()
        }

    }

    override val uiReceiver: ItemFetcherUiReceiver = ItemFetcherUiReceiver()

    override suspend fun SendChannel<SourceData>.initChannelFlow() {
        uiReceiver
            .flow
            .flowOn(Dispatchers.Main)
            .simpleScan(null) { previousSnapshot: ItemFetcherSnapshot<Param, Item>?, _: Unit? ->
                previousSnapshot?.close()
                ItemFetcherSnapshot(
                    displayedData = sourceDisplayedData,
                    paramProvider = getParamProvider(),
                    preShowLoader = getPreShowLoader(),
                    loader = getLoader(),
                    fetchDispatcherProvider = getFetchDispatcherProvider(),
                    processDataDispatcherProvider = getProcessDataDispatcherProvider(),
                    onRefreshComplete = uiReceiver::onRefreshComplete,
                    delegate = itemSource.delegate
                )
            }
            .filterNotNull()
            .simpleMapLatest { snapshot: ItemFetcherSnapshot<Param, Item> ->
                val downstreamFlow = snapshot.sourceEventFlow
                SourceData(downstreamFlow, uiReceiver)
            }
            .collect {
                send(it)
            }
    }

    private fun getRefreshStrategy(): RefreshStrategy {
        return itemSource.getRefreshStrategy()
    }

    private fun getParamProvider(): ParamProvider<Param> = ::getParam
    private fun getFetchDispatcherProvider(): DispatcherProvider<Param, Item> = ::getFetchDispatcher
    private fun getProcessDataDispatcherProvider(): DispatcherProvider<Param, Item> = ::getProcessDataDispatcher
    private fun getPreShowLoader(): PreShowLoader<Param, Item> = ::getPreShow
    private fun getLoader(): ItemLoader<Param, Item> = ::load

    private suspend fun getParam(): Param {
        return itemSource.getParam()
    }

    private fun getFetchDispatcher(param: Param, displayedData: SourceDisplayedData<Item>): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param, displayedData)
    }

    private fun getProcessDataDispatcher(param: Param, displayedData: SourceDisplayedData<Item>): CoroutineDispatcher {
        return itemSource.getProcessDataDispatcher(param, displayedData)
    }

    private suspend fun getPreShow(params: ItemSource.PreShowParams<Param, Item>): ItemSource.PreShowResult<Item> {
        return itemSource.getPreShow(params)
    }

    private suspend fun load(params: ItemSource.LoadParams<Param, Item>): ItemSource.LoadResult<Item> {
        return itemSource.load(params)
    }
}


class SourceResultProcessorGenerator<Param : Any, Item : Any>(
    private val sourceDisplayedData: SourceDisplayedData<Item>,
    private val items: List<Item>,
    private val resultExtra: Any?,
    private val dispatcher: CoroutineDispatcher?,
    private val delegate: BaseItemSource.Delegate<Param, Item>
) {


    val processor: SourceResultProcessor = {
        if (shouldUseDispatcher()) {
            withContext(dispatcher!!) {
                processResult()
            }
        } else {
            processResult()
        }
    }

    private fun shouldUseDispatcher(): Boolean {
        return (dispatcher != null
                && !sourceDisplayedData.originalItems.isNullOrEmpty()
                && items.isNotEmpty())
    }

    private fun processResult(): SourceProcessedResult {
        val updateResult = ListUpdate.calculateDiff(
            sourceDisplayedData.originalItems,
            items,
            delegate.getElementDiff()
        )

        val flatListItems = delegate.mapItems(updateResult.resultList)

        delegate.onProcessResult(
            updateResult.resultList,
            resultExtra,
            sourceDisplayedData
        )

        return SourceProcessedResult(flatListItems, updateResult.listOperates) {
            sourceDisplayedData.originalItems = updateResult.resultList
            sourceDisplayedData.flatListItems = flatListItems
            sourceDisplayedData.resultExtra = resultExtra

            flatListItems.forEach {
                it.delegate.bindParentLifecycle(delegate.lifecycleOwner.lifecycle)
                it.delegate.displayedItems = flatListItems
            }

            delegate.run {
                onItemsRecycled(updateResult.elementOperates.removedElements)
                onItemsChanged(updateResult.elementOperates.changedElements)
                onItemsDisplayed(updateResult.elementOperates.addedElements)
            }

            delegate.onResultDisplayed(sourceDisplayedData)
        }
    }
}


class ItemFetcherSnapshot<Param : Any, Item : Any>(
    private val displayedData: SourceDisplayedData<Item>,
    private val paramProvider: ParamProvider<Param>,
    private val preShowLoader: PreShowLoader<Param, Item>,
    private val loader: ItemLoader<Param, Item>,
    private val fetchDispatcherProvider: DispatcherProvider<Param, Item>,
    private val processDataDispatcherProvider: DispatcherProvider<Param, Item>,
    private val onRefreshComplete: Invoke,
    private val delegate: BaseItemSource.Delegate<Param, Item>
) {

    companion object {
        private const val TAG = "ItemFetcherSnapshot"
    }

    @Volatile
    private var closed = false
    private val sourceEventChannelFlowJob = Job()
    private val sourceEventCh = Channel<SourceEvent>(Channel.BUFFERED)


    val sourceEventFlow: Flow<SourceEvent> = cancelableChannelFlow(sourceEventChannelFlowJob) {
        launch {
            sourceEventCh.consumeAsFlow().collect {
                try {
                    if (closed) return@collect
                    send(it)
                } catch (e: ClosedSendChannelException) {
                }
            }
        }

        sourceEventCh.send(SourceEvent.Loading())
        val param = paramProvider.invoke()

        val preShowing = handlePreShowStep(param)

        handleLoadStep(param, preShowing)
    }

    private suspend fun handlePreShowStep(param: Param): Boolean {
        val preShowParams =
            ItemSource.PreShowParams(
                param,
                displayedData
            )
        val preShowResult = preShowLoader.invoke(preShowParams)

        var preShowing = false
        if (preShowResult is ItemSource.PreShowResult.Success<Item>) {
            preShowing = true
            val event = SourceEvent.PreShowing(
                SourceResultProcessorGenerator(
                    displayedData,
                    preShowResult.items,
                    preShowResult.resultExtra,
                    processDataDispatcherProvider.invoke(param, displayedData),
                    delegate
                ).processor
            )
            sourceEventCh.send(event)
        }
        return preShowing
    }

    private suspend fun handleLoadStep(param: Param, preShowing: Boolean) {
        val fetchDispatcher = fetchDispatcherProvider.invoke(param, displayedData)

        val loadParams = ItemSource.LoadParams(
            param,
            displayedData
        )
        val loadResult: ItemSource.LoadResult<Item>
        loadResult = if (fetchDispatcher == null) {
            loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loader.invoke(loadParams)
            }
        }

        when (loadResult) {
            is ItemSource.LoadResult.Success -> {
                val event = SourceEvent.RefreshSuccess(
                    SourceResultProcessorGenerator(
                        displayedData,
                        loadResult.items,
                        loadResult.resultExtra,
                        processDataDispatcherProvider.invoke(param, displayedData),
                        delegate
                    ).processor
                ) {
                    onRefreshComplete()
                }
                sourceEventCh.send(event)
            }
            is ItemSource.LoadResult.Error -> {
                val event = SourceEvent.RefreshError(loadResult.error, preShowing) {
                    onRefreshComplete()
                }
                sourceEventCh.send(event)
            }
        }
    }

    fun close() {
        closed = true
        Log.d(TAG, "ItemFetcherSnapshot close: ")
        sourceEventChannelFlowJob.cancel()
    }
}

internal typealias ParamProvider<Param> = (suspend () -> Param)
internal typealias PreShowLoader<Param, Item> = (suspend (params: ItemSource.PreShowParams<Param, Item>) -> ItemSource.PreShowResult<Item>)
internal typealias ItemLoader<Param, Item> = (suspend (param: ItemSource.LoadParams<Param, Item>) -> ItemSource.LoadResult<Item>)
internal typealias DispatcherProvider<Param, Item> = ((Param, SourceDisplayedData<Item>) -> CoroutineDispatcher?)