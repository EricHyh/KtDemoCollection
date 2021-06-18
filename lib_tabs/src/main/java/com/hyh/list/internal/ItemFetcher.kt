package com.hyh.list.internal

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


class ItemFetcher<Param : Any>(
    private val itemSource: ItemSource<Param>,
    private val initialParam: Param
) {

    private val uiReceiver = object : UiReceiverForSource<Param> {

        private val state = MutableStateFlow(Pair<Long, Param?>(0, null))

        val flow = state
            .map { it.second }
            .filterNotNull()

        override fun refresh(param: Param) {
            state.value = Pair(state.value.first + 1, param)
        }
    }

    val flow: Flow<SourceData<Param>> = simpleChannelFlow<SourceData<Param>> {
        uiReceiver
            .flow
            .onStart {
                emit(initialParam)
            }
            .flowOn(Dispatchers.Main)
            .simpleScan(null) { previousSnapshot: ItemFetcherSnapshot<Param>?, param: Param ->
                previousSnapshot?.close()
                ItemFetcherSnapshot(
                    param = param,
                    lastPreShowResult = previousSnapshot?.preShowResult,
                    lastLoadResult = previousSnapshot?.loadResult,
                    preShowLoader = getPreShowLoader(),
                    onPreShowResult = getOnPreShowResult(),
                    loader = getLoader(),
                    onItemLoadResult = getOnLoadResult(),
                    fetchDispatcher = getFetchDispatcher(param),
                    displayItems = previousSnapshot?.displayItems
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

    fun refresh(param: Param) {
        uiReceiver.refresh(param)
    }

    private fun getPreShowLoader(): PreShowLoader<Param> = ::getPreShow
    private fun getOnPreShowResult(): OnPreShowResult<Param> = ::onPreShowResult

    private fun getLoader(): ItemLoader<Param> = ::load
    private fun getOnLoadResult(): OnItemLoadResult<Param> = ::onLoadResult


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

    private fun getFetchDispatcher(param: Param): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param)
    }
}


class ItemFetcherSnapshot<Param : Any>(
    private val param: Param,
    private val lastPreShowResult: ItemSource.PreShowResult? = null,
    private val lastLoadResult: ItemSource.LoadResult? = null,
    private val preShowLoader: PreShowLoader<Param>,
    private val onPreShowResult: OnPreShowResult<Param>,
    private val loader: ItemLoader<Param>,
    private val onItemLoadResult: OnItemLoadResult<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
    var displayItems: List<ItemData>?
) {

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

        val preShowParams = ItemSource.PreShowParams(param, getDisplayItemsSnapshot(), lastPreShowResult, lastLoadResult)
        val preShowResult = preShowLoader.invoke(preShowParams)
        this@ItemFetcherSnapshot.preShowResult = preShowResult

        var preShowing = false
        if (preShowResult is ItemSource.PreShowResult.Success) {
            preShowing = true
            val items = ArrayList(preShowResult.items)
            val event = SourceEvent.PreShowing(items) {
                displayItems = items
            }
            sourceEventCh.send(event)
        }
        onPreShowResult(preShowParams, preShowResult)

        val loadParams = ItemSource.LoadParams(param, getDisplayItemsSnapshot(), lastPreShowResult, lastLoadResult)
        val loadResult: ItemSource.LoadResult
        if (fetchDispatcher == null) {
            loadResult = loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loadResult = loader.invoke(loadParams)
            }
        }
        this@ItemFetcherSnapshot.loadResult = loadResult

        when (loadResult) {
            is ItemSource.LoadResult.Success -> {
                val items = ArrayList(loadResult.items)
                val event = SourceEvent.Success(items) {
                    displayItems = items
                }
                sourceEventCh.send(event)
            }
            is ItemSource.LoadResult.Error -> {
                val event = SourceEvent.Error(loadResult.error, preShowing)
                sourceEventCh.send(event)
            }
        }
        onItemLoadResult(loadParams, loadResult)
    }

    fun close() {
        sourceEventChannelFlowJob.cancel()
    }

    private fun getDisplayItemsSnapshot(): List<ItemData>? {
        val displayItems = this.displayItems ?: return null
        return ArrayList(displayItems)
    }
}

internal typealias PreShowLoader<Param> = (suspend (params: ItemSource.PreShowParams<Param>) -> ItemSource.PreShowResult)
internal typealias OnPreShowResult<Param> = (suspend (ItemSource.PreShowParams<Param>, ItemSource.PreShowResult) -> Unit)
internal typealias ItemLoader<Param> = (suspend (param: ItemSource.LoadParams<Param>) -> ItemSource.LoadResult)
internal typealias OnItemLoadResult<Param> = (suspend (ItemSource.LoadParams<Param>, ItemSource.LoadResult) -> Unit)