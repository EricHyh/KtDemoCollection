package com.hyh.tabs.internal

import com.hyh.Invoke
import com.hyh.base.RefreshEventHandler
import com.hyh.base.RefreshStrategy
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.internal.utils.IElementDiff
import com.hyh.list.internal.utils.ListUpdate
import com.hyh.tabs.ITab
import com.hyh.tabs.TabInfo
import com.hyh.tabs.TabSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.collections.LinkedHashMap

abstract class TabFetcher<Param : Any, Tab : ITab>(private val initialParam: Param?) {

    private val displayedData = SourceDisplayedData<Tab>()

    private val uiReceiver = object : UiReceiver<Param> {

        private val refreshEventHandler = object : RefreshEventHandler<Param?>(initialParam) {

            override fun getRefreshStrategy(): RefreshStrategy {
                return this@TabFetcher.getRefreshStrategy()
            }
        }

        val flow = refreshEventHandler.flow

        override fun refresh(param: Param) {
            refreshEventHandler.onReceiveRefreshEvent(false, param)
        }

        fun onRefreshComplete() {
            refreshEventHandler.onRefreshComplete()
        }

        override fun destroy() {
            refreshEventHandler.onDestroy()
            onDestroy()
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
                    displayedData,
                    getCacheLoader(),
                    getLoader(),
                    if (param == null) Dispatchers.Unconfined else getFetchDispatcher(param),
                    uiReceiver::onRefreshComplete
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

    private fun getLoader(): TabLoader<Param, Tab> = ::load

    abstract fun getRefreshStrategy(): RefreshStrategy

    abstract suspend fun getCache(params: TabSource.CacheParams<Param, Tab>): TabSource.CacheResult<Tab>

    abstract suspend fun load(params: TabSource.LoadParams<Param, Tab>): TabSource.LoadResult<Tab>

    abstract fun getFetchDispatcher(param: Param): CoroutineDispatcher

    abstract fun onDestroy()
}


internal class TabSourceResultProcessorGenerator<Tab : ITab>(
    private val displayedData: SourceDisplayedData<Tab>,
    private var tabs: List<TabInfo<Tab>>,
    private val resultExtra: Any?
) {


    val processor: TabSourceResultProcessor<Tab> = {
        processResult()
    }

    private fun processResult(): TabSourceProcessedResult<Tab> {
        val oldTabs: List<TabInfo<Tab>>? = displayedData.tabs
        val tabMap = tabs.associateByTo(LinkedHashMap()) { it.tabToken }
        tabMap.forEach {

        }
        // TODO: eriche 2021/12/24  
        val diffResult = ListUpdate.calculateDiff(oldTabs, tabs, IElementDiff.AnyDiff())
        //val changed: Boolean = !Arrays.equals(oldTabs?.toTypedArray(), newTabs.toTypedArray())
        return TabSourceProcessedResult(
            diffResult.resultList,
            diffResult.listOperates.isNotEmpty()
        ) {
            displayedData.tabs = diffResult.resultList
            displayedData.resultExtra = resultExtra
        }
    }
}


internal class TabFetcherSnapshot<Param : Any, Tab : ITab>(
    private val param: Param?,
    private val displayedData: SourceDisplayedData<Tab>,
    private val cacheLoader: TabCacheLoader<Param, Tab>,
    private val loader: TabLoader<Param, Tab>,
    private val fetchDispatcher: CoroutineDispatcher?,
    private val onRefreshComplete: Invoke
) {
    private val pageEventChannelFlowJob = Job()
    private val pageEventCh = Channel<TabEvent<Tab>>(Channel.BUFFERED)


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

        val cacheParams = TabSource.CacheParams(param, displayedData)
        val cacheResult = cacheLoader.invoke(cacheParams)

        var usingCache = false
        if (cacheResult is TabSource.CacheResult.Success) {
            usingCache = true
            val event = TabEvent.UsingCache(
                TabSourceResultProcessorGenerator(displayedData, cacheResult.tabs, cacheResult.resultExtra).processor
            )
            pageEventCh.send(event)
        }

        val loadParams = TabSource.LoadParams(param, displayedData)
        val loadResult: TabSource.LoadResult<Tab>
        loadResult = if (fetchDispatcher == null) {
            loader.invoke(loadParams)
        } else {
            withContext(fetchDispatcher) {
                loader.invoke(loadParams)
            }
        }

        when (loadResult) {
            is TabSource.LoadResult.Success<Tab> -> {
                val event = TabEvent.Success(
                    TabSourceResultProcessorGenerator(displayedData, loadResult.tabs, loadResult.resultExtra).processor
                ) {
                    onRefreshComplete()
                }
                pageEventCh.send(event)
            }
            is TabSource.LoadResult.Error<Tab> -> {
                val event = TabEvent.Error<Tab>(loadResult.error, usingCache) {
                    onRefreshComplete()
                }
                pageEventCh.send(event)
            }
        }
    }

    fun close() {
        pageEventChannelFlowJob.cancel()
    }
}

internal typealias TabCacheLoader<Param, Tab> = (suspend (TabSource.CacheParams<Param, Tab>) -> TabSource.CacheResult<Tab>)
internal typealias TabLoader<Param, Tab> = (suspend (TabSource.LoadParams<Param, Tab>) -> TabSource.LoadResult<Tab>)