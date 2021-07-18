package com.hyh.list.internal

import android.util.Log
import com.hyh.*
import com.hyh.base.RefreshEventHandler
import com.hyh.base.RefreshStrategy
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.ItemSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*


class ItemFetcher<Param : Any, Item : Any>(
    private val itemSource: ItemSource<Param, Item>,
) {

    private val sourceDisplayedData = SourceDisplayedData<Item>()

    private val uiReceiver = object : UiReceiverForSource {

        private val refreshEventHandler = object : RefreshEventHandler<Unit>(Unit) {

            override fun getRefreshStrategy(): RefreshStrategy {
                return this@ItemFetcher.getRefreshStrategy()
            }
        }

        val flow = refreshEventHandler.flow.map { it.first }

        override fun refresh(important: Boolean) {
            refreshEventHandler.onReceiveRefreshEvent(important, Unit)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onRefreshComplete()
        }

        override fun destroy() {
            refreshEventHandler.onDestroy()
        }
    }

    val flow: Flow<SourceData> = simpleChannelFlow<SourceData> {
        uiReceiver
            .flow
            .flowOn(Dispatchers.Main)
            .simpleScan(null) { previousSnapshot: ItemFetcherSnapshot<Param, Item>?, id: Long ->
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
    }.buffer(Channel.BUFFERED)

    fun refresh(important: Boolean) {
        uiReceiver.refresh(important)
    }

    private fun getRefreshStrategy(): RefreshStrategy {
        return itemSource.getRefreshStrategy()
    }

    private fun getParamProvider(): ParamProvider<Param> = ::getParam
    private fun getFetchDispatcherProvider(): DispatcherProvider<Param> = ::getFetchDispatcher
    private fun getProcessDataDispatcherProvider(): DispatcherProvider<Param> = ::getProcessDataDispatcher
    private fun getPreShowLoader(): PreShowLoader<Param, Item> = ::getPreShow
    private fun getLoader(): ItemLoader<Param, Item> = ::load

    private suspend fun getParam(): Param {
        return itemSource.getParam()
    }

    private fun getFetchDispatcher(param: Param): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param)
    }

    private fun getProcessDataDispatcher(param: Param): CoroutineDispatcher {
        return itemSource.getProcessDataDispatcher(param)
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
    private val delegate: ItemSource.Delegate<Param, Item>,
) {

    companion object {
        private const val TAG = "ResultProcessor"
    }

    val processor: SourceResultProcessor = {
        if (dispatcher != null) {
            withContext(dispatcher) {
                processResult()
            }
        } else {
            processResult()
        }
    }

    private fun processResult(): SourceProcessedResult {

        val updateResult = ListUpdate.calculateDiff(
            sourceDisplayedData.items,
            items,
            delegate.getElementDiff()
        )

        val itemDataList = delegate.mapItems(updateResult.resultList)

        delegate.onProcessResult(
            updateResult.resultList,
            resultExtra,
            sourceDisplayedData
        )

        return SourceProcessedResult(itemDataList, updateResult.listOperates) {
            sourceDisplayedData.items = updateResult.resultList
            sourceDisplayedData.itemDataList = itemDataList
            sourceDisplayedData.resultExtra = resultExtra

            itemDataList.forEach {
                it.delegate.displayedItems = itemDataList
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
    private val fetchDispatcherProvider: DispatcherProvider<Param>,
    private val processDataDispatcherProvider: DispatcherProvider<Param>,
    private val onRefreshComplete: Invoke,
    private val delegate: ItemSource.Delegate<Param, Item>
) {

    companion object {
        private const val TAG = "ItemFetcher"
    }


    var preShowResult: ItemSource.PreShowResult<Item>? = null
    var loadResult: ItemSource.LoadResult<Item>? = null

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
        this@ItemFetcherSnapshot.preShowResult = preShowResult

        var preShowing = false
        if (preShowResult is ItemSource.PreShowResult.Success<Item>) {
            preShowing = true
            val event = SourceEvent.PreShowing(
                SourceResultProcessorGenerator(
                    displayedData,
                    preShowResult.items,
                    preShowResult.resultExtra,
                    processDataDispatcherProvider.invoke(param),
                    delegate
                ).processor
            )
            sourceEventCh.send(event)
        }
        return preShowing
    }

    private suspend fun handleLoadStep(param: Param, preShowing: Boolean) {
        val fetchDispatcher = fetchDispatcherProvider.invoke(param)

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
        this@ItemFetcherSnapshot.loadResult = loadResult

        when (loadResult) {
            is ItemSource.LoadResult.Success -> {
                val event = SourceEvent.RefreshSuccess(
                    SourceResultProcessorGenerator(
                        displayedData,
                        loadResult.items,
                        loadResult.resultExtra,
                        processDataDispatcherProvider.invoke(param),
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
        sourceEventChannelFlowJob.cancel()
    }
}

internal typealias ParamProvider<Param> = (suspend () -> Param)
internal typealias PreShowLoader<Param, Item> = (suspend (params: ItemSource.PreShowParams<Param, Item>) -> ItemSource.PreShowResult<Item>)
internal typealias ItemLoader<Param, Item> = (suspend (param: ItemSource.LoadParams<Param, Item>) -> ItemSource.LoadResult<Item>)
internal typealias DispatcherProvider<Param> = ((Param) -> CoroutineDispatcher?)