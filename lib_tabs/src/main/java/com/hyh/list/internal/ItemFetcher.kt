package com.hyh.list.internal

import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.IItemSource
import com.hyh.list.LoadParams
import com.hyh.list.PreShowParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ItemFetcher<Param : Any>(
    private val itemSource: IItemSource<Param>,
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
            .simpleScan(null) { previousSnapshot: ItemFetcherSnapshot<Param>?, param: Param ->
                previousSnapshot?.close()
                ItemFetcherSnapshot(
                    param = param,
                    lastPreShowResult = previousSnapshot?.preShowResult,
                    lastLoadResult = previousSnapshot?.loadResult,
                    preShowLoader = getPreShowLoader(),
                    loader = getLoader(),
                    fetchDispatcher = getFetchDispatcher(param)
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

    private fun getPreShowLoader(): PreShowLoader<Param> = ::getPreShow

    private fun getLoader(): ItemLoader<Param> = ::load

    private suspend fun getPreShow(params: PreShowParams<Param>): IItemSource.PreShowResult {
        return itemSource.getPreShow(params)
    }

    private suspend fun load(params: LoadParams<Param>): IItemSource.LoadResult {
        return itemSource.load(params)
    }

    private fun getFetchDispatcher(param: Param): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param)
    }
}


class ItemFetcherSnapshot<Param : Any>(
    private val param: Param,
    private val lastPreShowResult: IItemSource.PreShowResult? = null,
    private val lastLoadResult: IItemSource.LoadResult? = null,
    private val preShowLoader: PreShowLoader<Param>,
    private val loader: ItemLoader<Param>,
    private val fetchDispatcher: CoroutineDispatcher?,
) {

    var preShowResult: IItemSource.PreShowResult? = null
    var loadResult: IItemSource.LoadResult? = null

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

        sourceEventCh.send(SourceEvent.Loading)

        val preShowResult = preShowLoader.invoke(PreShowParams(param, lastPreShowResult, lastLoadResult))
        this@ItemFetcherSnapshot.preShowResult = preShowResult

        var preShowing = false
        if (preShowResult is IItemSource.PreShowResult.Success) {
            preShowing = true
            val event = SourceEvent.PreShowing(ArrayList(preShowResult.items))
            sourceEventCh.send(event)
        }

        val loadResult: IItemSource.LoadResult
        if (fetchDispatcher == null) {
            loadResult = loader.invoke(LoadParams(param, lastPreShowResult, lastLoadResult))
        } else {
            withContext(fetchDispatcher) {
                loadResult = loader.invoke(LoadParams(param, lastPreShowResult, lastLoadResult))
            }
        }
        this@ItemFetcherSnapshot.loadResult = loadResult

        when (loadResult) {
            is IItemSource.LoadResult.Success -> {
                val event = SourceEvent.Success(ArrayList(loadResult.items))
                sourceEventCh.send(event)
            }
            is IItemSource.LoadResult.Error -> {
                val event = SourceEvent.Error(loadResult.error, preShowing)
                sourceEventCh.send(event)
            }
        }
    }

    fun close() {
        sourceEventChannelFlowJob.cancel()
    }
}

internal typealias PreShowLoader<Param> = (suspend (params: PreShowParams<Param>) -> IItemSource.PreShowResult)
internal typealias ItemLoader<Param> = (suspend (param: LoadParams<Param>) -> IItemSource.LoadResult)