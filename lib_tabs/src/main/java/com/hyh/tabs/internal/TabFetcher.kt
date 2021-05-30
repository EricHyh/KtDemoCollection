package com.hyh.tabs.internal

import android.util.Log
import com.hyh.tabs.ITab
import com.hyh.tabs.TabInfo
import com.hyh.tabs.TabSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

abstract class TabFetcher<Param : Any, Tab : ITab>(private val initialParam: Param?) {

    private val uiReceiver = object : UiReceiver<Param> {

        private val state = MutableStateFlow(Pair<Long, Param?>(0, null))

        val flow = state.map { it.second }

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
            .simpleScan(null) { previousSnapshot: TabFetcherSnapshot<Param, Tab>?, param: Param? ->
                previousSnapshot?.close()
                val completeTimes = (previousSnapshot?.completeTimes ?: 0) + (if (previousSnapshot?.getCacheComplete == true) 1 else 0)
                val snapshot: TabFetcherSnapshot<Param, Tab> = if (param == null) {
                    TabFetcherSnapshot(param, completeTimes, getCacheLoader(), getLoader(), null)
                } else {
                    TabFetcherSnapshot(param, completeTimes, getCacheLoader(), getLoader(), getFetchDispatcher(param))
                }
                snapshot
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

    private fun getCacheLoader(): CacheTabLoader<Param, Tab> = ::getCache

    private fun getLoader(): TabLoader<Param, Tab> = ::load

    abstract suspend fun getCache(param: Param, completeTimes: Int): TabSource.CacheResult<Tab>

    abstract suspend fun load(param: Param): TabSource.LoadResult<Tab>

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher
}


internal class TabFetcherSnapshot<Param : Any, Tab : ITab>(
    private val param: Param?,
    val completeTimes: Int,
    private val cacheLoader: CacheTabLoader<Param, Tab>,
    private val loader: TabLoader<Param, Tab>,
    private val fetchDispatcher: CoroutineDispatcher?,
) {
    private val pageEventChannelFlowJob = Job()
    private val pageEventCh = Channel<TabEvent<Tab>>(Channel.BUFFERED)

    var getCacheComplete = false

    val pageEventFlow: Flow<TabEvent<Tab>> = cancelableChannelFlow(pageEventChannelFlowJob) {
        launch {
            pageEventCh.consumeAsFlow().collect {
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

        pageEventCh.send(TabEvent.Loading())

        val cacheResult = cacheLoader.invoke(param, completeTimes)
        var usingCache = false
        if (cacheResult is TabSource.CacheResult.Success) {
            usingCache = true
            val event = TabEvent.UsingCache(ArrayList<TabInfo<Tab>>())
            pageEventCh.send(event)
            getCacheComplete = true
        }

        val loadResult: TabSource.LoadResult<Tab>
        if (fetchDispatcher == null) {
            loadResult = loader.invoke(param)
        } else {
            withContext(fetchDispatcher) {
                loadResult = loader.invoke(param)
            }
        }

        when (loadResult) {
            is TabSource.LoadResult.Success<Tab> -> {
                val event = TabEvent.Success(loadResult.tabs)
                pageEventCh.send(event)
            }
            is TabSource.LoadResult.Error<Tab> -> {
                val event = TabEvent.Error<Tab>(loadResult.error, usingCache)
                pageEventCh.send(event)
            }
        }
    }

    fun close() {
        pageEventChannelFlowJob.cancel()
    }
}

internal typealias CacheTabLoader<Param, Tab> = (suspend (param: Param, completeTimes: Int) -> TabSource.CacheResult<Tab>)
internal typealias TabLoader<Param, Tab> = (suspend (param: Param) -> TabSource.LoadResult<Tab>)
