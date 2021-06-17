package com.hyh.tabs.internal

import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.tabs.ITab
import com.hyh.tabs.TabSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*

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
                TabFetcherSnapshot(
                    param,
                    previousSnapshot?.cacheResult,
                    previousSnapshot?.loadResult,
                    getCacheLoader(),
                    getOnCacheResult(),
                    getLoader(),
                    getOnLoadResult(),
                    if (param == null) Dispatchers.Unconfined else getFetchDispatcher(param)
                )
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

    private fun getCacheLoader(): TabCacheLoader<Param, Tab> = ::getCache
    private fun getOnCacheResult(): OnTabCacheResult<Param, Tab> = ::onCacheResult
    private fun getLoader(): TabLoader<Param, Tab> = ::load
    private fun getOnLoadResult(): OnTabLoadResult<Param, Tab> = ::onLoadResult

    abstract suspend fun getCache(params: TabSource.CacheParams<Param, Tab>): TabSource.CacheResult<Tab>
    abstract suspend fun onCacheResult(params: TabSource.CacheParams<Param, Tab>, result: TabSource.CacheResult<Tab>)

    abstract suspend fun load(params: TabSource.LoadParams<Param, Tab>): TabSource.LoadResult<Tab>
    abstract suspend fun onLoadResult(params: TabSource.LoadParams<Param, Tab>, result: TabSource.LoadResult<Tab>)

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher
}


internal class TabFetcherSnapshot<Param : Any, Tab : ITab>(
    private val param: Param?,
    private val lastCacheResult: TabSource.CacheResult<Tab>? = null,
    private val lastLoadResult: TabSource.LoadResult<Tab>? = null,
    private val cacheLoader: TabCacheLoader<Param, Tab>,
    private val onTabCacheResult: OnTabCacheResult<Param, Tab>,
    private val loader: TabLoader<Param, Tab>,
    private val onTabLoadResult: OnTabLoadResult<Param, Tab>,
    private val fetchDispatcher: CoroutineDispatcher?,
) {
    private val pageEventChannelFlowJob = Job()
    private val pageEventCh = Channel<TabEvent<Tab>>(Channel.BUFFERED)

    var cacheResult: TabSource.CacheResult<Tab>? = null
    var loadResult: TabSource.LoadResult<Tab>? = null

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

        val cacheParams = TabSource.CacheParams(param, lastCacheResult, lastLoadResult)
        val cacheResult = cacheLoader.invoke(cacheParams)
        this@TabFetcherSnapshot.cacheResult = cacheResult

        var usingCache = false
        if (cacheResult is TabSource.CacheResult.Success) {
            usingCache = true
            val event = TabEvent.UsingCache(ArrayList(cacheResult.tabs))
            pageEventCh.send(event)
        }
        onTabCacheResult(cacheParams, cacheResult)

        val loadParams = TabSource.LoadParams(param, lastCacheResult, lastLoadResult)
        val loadResult: TabSource.LoadResult<Tab>
        if (fetchDispatcher == null) {
            loadResult = loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loadResult = loader.invoke(loadParams)
            }
        }
        this@TabFetcherSnapshot.loadResult = loadResult

        when (loadResult) {
            is TabSource.LoadResult.Success<Tab> -> {
                val event = TabEvent.Success(ArrayList(loadResult.tabs))
                pageEventCh.send(event)
            }
            is TabSource.LoadResult.Error<Tab> -> {
                val event = TabEvent.Error<Tab>(loadResult.error, usingCache)
                pageEventCh.send(event)
            }
        }
        onTabLoadResult(loadParams, loadResult)
    }

    fun close() {
        pageEventChannelFlowJob.cancel()
    }
}

internal typealias TabCacheLoader<Param, Tab> = (suspend (TabSource.CacheParams<Param, Tab>) -> TabSource.CacheResult<Tab>)
internal typealias OnTabCacheResult<Param, Tab> = (suspend (TabSource.CacheParams<Param, Tab>, TabSource.CacheResult<Tab>) -> Unit)
internal typealias TabLoader<Param, Tab> = (suspend (TabSource.LoadParams<Param, Tab>) -> TabSource.LoadResult<Tab>)
internal typealias OnTabLoadResult<Param, Tab> = (suspend (TabSource.LoadParams<Param, Tab>, TabSource.LoadResult<Tab>) -> Unit)