package com.hyh.list.internal

import android.util.Log
import com.hyh.*
import com.hyh.base.RefreshEventHandler
import com.hyh.base.RefreshStrategy
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.FlatListItem
import com.hyh.list.ItemSource
import com.hyh.list.internal.base.*
import com.hyh.list.internal.base.DispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*


class ItemFetcher<Param, Item>(
    override val itemSource: ItemSource<Param, Item>
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

        override fun append(important: Boolean) {
            refreshEventHandler.onReceiveRefreshEvent(important, Unit)
        }

        override fun rearrange(important: Boolean) {
            refreshEventHandler.onReceiveRefreshEvent(important, Unit)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onRefreshComplete()
        }

        override fun getDisplayedItems(): List<FlatListItem> {
            return sourceDisplayedData.flatListItems ?: emptyList()
        }

        override fun destroy() {
            super.destroy()
            refreshEventHandler.onDestroy()
        }
    }

    override val uiReceiver: ItemFetcherUiReceiver = ItemFetcherUiReceiver()

    override suspend fun SendChannel<SourceData>.initChannelFlow() {
        coroutineScope {
            launch {
                uiReceiver
                    .flow
                    .flowOn(Dispatchers.Main)
                    .simpleScan(null) { previousSnapshot: IItemFetcherSnapshot?, _: Unit? ->
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
                    .simpleMapLatest { snapshot: IItemFetcherSnapshot ->
                        val downstreamFlow = snapshot.sourceEventFlow
                        SourceData(downstreamFlow, uiReceiver)
                    }
                    .collect {
                        send(it)
                    }
            }
        }
    }

    private fun getRefreshStrategy(): RefreshStrategy {
        return itemSource.getRefreshStrategy()
    }

    private fun getParamProvider(): ParamProvider<Param> = ::getParam
    private fun getPreShowLoader(): PreShowLoader<Param, Item> = ::getPreShow
    private fun getLoader(): ItemLoader<Param, Item> = ::load

    private suspend fun getParam(): Param {
        return itemSource.getParam()
    }

    private suspend fun getPreShow(params: ItemSource.PreShowParams<Param>): ItemSource.PreShowResult<Item> {
        return itemSource.getPreShow(params)
    }

    private suspend fun load(params: ItemSource.LoadParams<Param>): ItemSource.LoadResult<Item> {
        return itemSource.load(params)
    }
}


class ItemFetcherSnapshot<Param, Item>(
    private val displayedData: SourceDisplayedData,
    private val paramProvider: ParamProvider<Param>,
    private val preShowLoader: PreShowLoader<Param, Item>,
    private val loader: ItemLoader<Param, Item>,
    private val fetchDispatcherProvider: DispatcherProvider<Param>,
    private val processDataDispatcherProvider: DispatcherProvider<Param>,
    private val onRefreshComplete: Invoke,
    private val delegate: BaseItemSource.Delegate<Param, Item>
) : IItemFetcherSnapshot {

    companion object {
        private const val TAG = "ItemFetcherSnapshot"
    }

    @Volatile
    private var closed = false
    private val sourceEventChannelFlowJob = Job()
    private val sourceEventCh = Channel<SourceEvent>(Channel.BUFFERED)


    override val sourceEventFlow: Flow<SourceEvent> =
        cancelableChannelFlow(sourceEventChannelFlowJob) {
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
                    null,
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
        val loadResult: ItemSource.LoadResult<Item> = if (fetchDispatcher == null) {
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
                        null,
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

    override fun close() {
        closed = true
        Log.d(TAG, "ItemFetcherSnapshot close: ")
        sourceEventChannelFlowJob.cancel()
    }
}

internal typealias ParamProvider<Param> = (suspend () -> Param)
internal typealias PreShowLoader<Param, Item> = (suspend (params: ItemSource.PreShowParams<Param>) -> ItemSource.PreShowResult<Item>)
internal typealias ItemLoader<Param, Item> = (suspend (param: ItemSource.LoadParams<Param>) -> ItemSource.LoadResult<Item>)