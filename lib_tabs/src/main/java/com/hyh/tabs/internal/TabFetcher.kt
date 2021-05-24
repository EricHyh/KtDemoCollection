package com.hyh.tabs.internal

import com.hyh.tabs.ITab
import com.hyh.tabs.TabSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class TabFetcher<Param : Any, Tab : ITab>(private val initialParam: Param) {

    private val uiReceiver = object : UiReceiver<Param> {

        private val state = MutableStateFlow(Pair<Int, Param?>(Integer.MIN_VALUE, null))

        val flow = state.mapNotNull { it.second }

        override fun refresh(param: Param) {
            state.value = Pair(state.value.first + 1, param)
        }
    }

    val flow: Flow<TabData<Param, Tab>> = simpleChannelFlow<TabData<Param, Tab>> {
        uiReceiver
            .flow
            .onStart {
                emit(initialParam)
            }
            .simpleScan(null) { previousSnapshot: TabFetcherSnapshot<Param, Tab>?, param: Param ->
                previousSnapshot?.close()
                TabFetcherSnapshot(param, getLoader(), getFetchDispatcher(param))
            }
            .filterNotNull()
            .simpleMapLatest { snapshot ->
                val downstreamFlow = snapshot.pageEventFlow
                TabData(downstreamFlow, uiReceiver)
            }
            .collect {
                send(it)
            }
    }.buffer(Channel.BUFFERED)

    private fun getLoader(): TabLoader<Param, Tab> = ::load

    abstract suspend fun load(param: Param): TabSource.LoadResult<Tab>

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher
}


internal class TabFetcherSnapshot<Param : Any, Tab : ITab>(
    private val param: Param,
    private val loader: TabLoader<Param, Tab>,
    private val fetchDispatcher: CoroutineDispatcher
) {
    private val pageEventChannelFlowJob = Job()
    private val pageEventCh = Channel<TabEvent<Tab>>(Channel.BUFFERED)

    val pageEventFlow: Flow<TabEvent<Tab>> = cancelableChannelFlow(pageEventChannelFlowJob) {
        launch {
            pageEventCh.consumeAsFlow().collect {
                // Protect against races where a subsequent call to submitData invoked close(),
                // but a pageEvent arrives after closing causing ClosedSendChannelException.
                try {
                    send(it)
                } catch (e: ClosedSendChannelException) {
                    // Safe to drop PageEvent here, since collection has been cancelled.
                }
            }
        }

        pageEventCh.send(TabEvent.Loading())

        val result: TabSource.LoadResult<Tab>
        withContext(fetchDispatcher) {
            result = loader.invoke(param)
        }

        when (result) {
            is TabSource.LoadResult.Success<Tab> -> {
                val event = TabEvent.Success(result.tabs)
                pageEventCh.send(event)
            }
            is TabSource.LoadResult.Error<Tab> -> {
                val event = TabEvent.Error<Tab>(result.error)
                pageEventCh.send(event)
            }
        }
    }

    fun close() {
        pageEventChannelFlowJob.cancel()
    }
}


internal typealias TabLoader<Param, Tab> = (suspend (param: Param) -> TabSource.LoadResult<Tab>)