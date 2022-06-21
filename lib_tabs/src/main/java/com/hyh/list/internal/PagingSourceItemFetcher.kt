package com.hyh.list.internal

import android.util.Log
import com.hyh.Invoke
import com.hyh.coroutine.SimpleMutableStateFlow
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.ItemPagingSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

class PagingSourceItemFetcher<Param : Any, Item : Any>(
    private val pagingSource: ItemPagingSource<Param, Item>
) : BaseItemFetcher<ItemPagingSource.LoadParams<Param>, Item>(pagingSource) {

    override val sourceDisplayedData: PagingSourceDisplayedData<Param, Item> = PagingSourceDisplayedData()

    inner class ItemFetcherUiReceiver : BaseUiReceiverForSource() {

        private val loadEventHandler = LoadEventHandler()

        val flow = loadEventHandler.flow

        override fun refresh(important: Boolean) {
            loadEventHandler.onReceiveLoadEvent(LoadEvent.Refresh)
        }

        override fun accessItem(position: Int) {
            if (sourceDisplayedData.noMore) return
            if (sourceDisplayedData.lastPaging == null) return
            val size = sourceDisplayedData.flatListItems?.size ?: 0
            if (position >= size - 4) {
                append()
            }
        }

        fun append() {
            loadEventHandler.onReceiveLoadEvent(LoadEvent.Append)
        }

        fun onRefreshComplete() {
            loadEventHandler.onLoadEventComplete(LoadEvent.Refresh)
        }

        fun onAppendComplete() {
            loadEventHandler.onLoadEventComplete(LoadEvent.Append)
        }
    }

    override val uiReceiver: ItemFetcherUiReceiver = ItemFetcherUiReceiver()

    override suspend fun SendChannel<SourceData>.initChannelFlow() {
        uiReceiver
            .flow
            .flowOn(Dispatchers.Main)
            .simpleScan(null) { previousSnapshot: PagingSourceItemFetcherSnapshot<Param, Item>?, loadEvent: LoadEvent ->
                if (sourceDisplayedData.noMore) return@simpleScan null
                previousSnapshot?.close()
                PagingSourceItemFetcherSnapshot(
                    displayedData = sourceDisplayedData,
                    refreshKeyProvider = getRefreshKeyProvider(),
                    appendKeyProvider = getAppendKeyProvider(),
                    loader = getPagingSourceLoader(),
                    onRefreshComplete = uiReceiver::onRefreshComplete,
                    onAppendComplete = uiReceiver::onAppendComplete,
                    delegate = itemSource.delegate,
                    fetchDispatcherProvider = getFetchDispatcherProvider(),
                    processDataDispatcherProvider = getProcessDataDispatcherProvider(),
                    forceRefresh = loadEvent == LoadEvent.Refresh
                )
            }
            .filterNotNull()
            .simpleMapLatest { snapshot: PagingSourceItemFetcherSnapshot<Param, Item> ->
                val downstreamFlow = snapshot.sourceEventFlow
                SourceData(downstreamFlow, uiReceiver)
            }
            .collect {
                send(it)
            }
    }

    private fun getPagingSourceLoader(): PagingSourceLoader<Param, Item> = ::load
    private fun getRefreshKeyProvider(): RefreshKeyProvider<Param> = ::getRefreshKey
    private fun getAppendKeyProvider(): AppendKeyProvider<Param> = ::getAppendKey


    private suspend fun load(param: ItemPagingSource.LoadParams<Param>): ItemPagingSource.LoadResult<Param, Item> {
        return pagingSource.load(param)
    }

    private suspend fun getRefreshKey(): Param? {
        return pagingSource.getRefreshKey() ?: pagingSource.initialParam
    }

    private suspend fun getAppendKey(): Param? {
        return sourceDisplayedData.appendParam
    }
}


class PagingSourceItemFetcherSnapshot<Param : Any, Item : Any>(
    private val displayedData: PagingSourceDisplayedData<Param, Item>,
    private val refreshKeyProvider: RefreshKeyProvider<Param>,
    private val appendKeyProvider: AppendKeyProvider<Param>,
    private val loader: PagingSourceLoader<Param, Item>,
    private val onRefreshComplete: Invoke,
    private val onAppendComplete: Invoke,
    private val delegate: BaseItemSource.Delegate<ItemPagingSource.LoadParams<Param>, Item>,
    private val fetchDispatcherProvider: DispatcherProvider<ItemPagingSource.LoadParams<Param>, Item>,
    private val processDataDispatcherProvider: DispatcherProvider<ItemPagingSource.LoadParams<Param>, Item>,
    private val forceRefresh: Boolean = false
) {


    companion object {
        private const val TAG = "PagingSourceItemFetcher"
    }

    @Volatile
    private var closed = false
    private val sourceEventChannelFlowJob = Job()
    private val sourceEventCh = Channel<SourceEvent>(Channel.BUFFERED)


    val sourceEventFlow: Flow<SourceEvent> = cancelableChannelFlow(sourceEventChannelFlowJob) {
        if (displayedData.noMore) return@cancelableChannelFlow

        launch {
            sourceEventCh.consumeAsFlow().collect {
                try {
                    if (closed) return@collect
                    send(it)
                } catch (e: ClosedSendChannelException) {
                }
            }
        }

        val isRefresh = displayedData.lastPaging == null || forceRefresh
        val param = if (isRefresh) {
            ItemPagingSource.LoadParams.Refresh(refreshKeyProvider.invoke())
        } else {
            ItemPagingSource.LoadParams.Append(appendKeyProvider.invoke())
        }

        val fetchDispatcher = fetchDispatcherProvider.invoke(param, displayedData)

        val loadResult: ItemPagingSource.LoadResult<Param, Item> = if (fetchDispatcher == null) {
            loader.invoke(param)
        } else {
            withContext(fetchDispatcher) {
                loader.invoke(param)
            }
        }

        when (loadResult) {
            is ItemPagingSource.LoadResult.Error -> {
                if (isRefresh) {
                    SourceEvent.PagingRefreshError(loadResult.throwable) {
                        onRefreshComplete()
                    }
                } else {
                    SourceEvent.PagingAppendError(loadResult.throwable, displayedData.pagingSize) {
                        onAppendComplete()
                    }
                }.apply {
                    sourceEventCh.send(this)
                }
            }
            is ItemPagingSource.LoadResult.Success -> {
                if (isRefresh) {
                    SourceEvent.PagingRefreshSuccess(refreshProcessor(param, loadResult), loadResult.noMore) {
                        onRefreshComplete()
                    }
                } else {
                    SourceEvent.PagingAppendSuccess(appendProcessor(param, loadResult), displayedData.pagingSize, loadResult.noMore) {
                        onAppendComplete()
                    }
                }.apply {
                    sourceEventCh.send(this)
                }
            }
        }
    }

    fun close() {
        closed = true
        Log.d(TAG, "PagingSourceItemFetcher close: ")
        sourceEventChannelFlowJob.cancel()
    }


    private fun refreshProcessor(
        param: ItemPagingSource.LoadParams<Param>,
        success: ItemPagingSource.LoadResult.Success<Param, Item>
    ): SourceResultProcessor {

        fun process(): SourceProcessedResult {
            val items = success.items
            val resultExtra = null
            val flatListItems = delegate.mapItems(items)
            val nextParam = success.nextParam
            val noMore = success.noMore

            delegate.onProcessResult(
                items,
                resultExtra,
                displayedData
            )

            return SourceProcessedResult(flatListItems, listOf(ListOperate.OnAllChanged)) {
                val originalItems = displayedData.originalItems
                displayedData.originalItems = items
                displayedData.flatListItems = flatListItems
                displayedData.resultExtra = resultExtra
                displayedData.pagingList = listOf(
                    Paging(
                        originalItems = items,
                        flatListItems = flatListItems,
                        param = param.param,
                        nextParam = nextParam,
                        noMore = noMore
                    )
                )
                flatListItems.forEach {
                    it.delegate.bindParentLifecycle(delegate.lifecycleOwner.lifecycle)
                    it.delegate.displayedItems = flatListItems
                }

                delegate.run {
                    if (originalItems?.isNotEmpty() == true) {
                        onItemsRecycled(originalItems)
                    }
                    onItemsDisplayed(items)
                }

                delegate.onResultDisplayed(displayedData)
            }

        }

        return run@{
            val dispatcher = processDataDispatcherProvider.invoke(param, displayedData)
            if (dispatcher != null && shouldUseDispatcher(success.items)) {
                return@run withContext(dispatcher) {
                    process()
                }
            } else {
                return@run process()
            }
        }
    }


    private fun appendProcessor(
        param: ItemPagingSource.LoadParams<Param>,
        success: ItemPagingSource.LoadResult.Success<Param, Item>
    ): SourceResultProcessor {

        fun process(): SourceProcessedResult {
            val items = success.items
            val resultExtra = null
            val flatListItems = delegate.mapItems(items)
            val nextParam = success.nextParam
            val noMore = success.noMore


            val oldItems = displayedData.originalItems ?: emptyList()
            val oldFlatListItems = displayedData.flatListItems ?: emptyList()

            val resultItems = oldItems + items
            val resultFlatListItems = oldFlatListItems + flatListItems


            delegate.onProcessResult(
                resultItems,
                resultExtra,
                displayedData
            )

            return SourceProcessedResult(resultFlatListItems, listOf(ListOperate.OnInserted(oldItems.size, items.size))) {
                displayedData.originalItems = resultItems
                displayedData.flatListItems = resultFlatListItems
                displayedData.resultExtra = resultExtra

                val pagingList = displayedData.pagingList

                displayedData.pagingList = pagingList + Paging(
                    originalItems = items,
                    flatListItems = flatListItems,
                    param = param.param,
                    nextParam = nextParam,
                    noMore = noMore
                )

                flatListItems.forEach {
                    it.delegate.bindParentLifecycle(delegate.lifecycleOwner.lifecycle)
                    it.delegate.displayedItems = flatListItems
                }

                delegate.run {
                    onItemsDisplayed(items)
                }

                delegate.onResultDisplayed(displayedData)
            }

        }

        return run@{
            val dispatcher = processDataDispatcherProvider.invoke(param, displayedData)
            if (dispatcher != null && shouldUseDispatcher(success.items)) {
                return@run withContext(dispatcher) {
                    process()
                }
            } else {
                return@run process()
            }
        }
    }


    private fun shouldUseDispatcher(
        items: List<Item>
    ): Boolean {
        return !displayedData.originalItems.isNullOrEmpty() && items.isNotEmpty()
    }
}


internal class LoadEventHandler {

    private val state = SimpleMutableStateFlow<Pair<Int, LoadEvent>>(0 to LoadEvent.Refresh)

    val flow: Flow<LoadEvent> = state.asStateFlow().map { it.second }

    private val refreshComplete: AtomicBoolean = AtomicBoolean(false)

    private val appendComplete: AtomicBoolean = AtomicBoolean(true)

    @Synchronized
    fun onReceiveLoadEvent(event: LoadEvent) {
        when (event) {
            LoadEvent.Refresh -> {
                if (refreshComplete.get() || state.value != event) {
                    refreshComplete.set(false)
                    state.value = Pair(state.value.first + 1, event)
                }
            }
            LoadEvent.Append -> {
                if (appendComplete.get() && refreshComplete.get()) {
                    appendComplete.set(false)
                    state.value = Pair(state.value.first + 1, event)
                }
            }
        }
    }

    @Synchronized
    fun onLoadEventComplete(event: LoadEvent) {
        when (event) {
            LoadEvent.Refresh -> {
                refreshComplete.set(true)
            }
            LoadEvent.Append -> {
                appendComplete.set(true)
            }
        }
    }
}


sealed class LoadEvent {

    object Refresh : LoadEvent()

    object Append : LoadEvent()

}



internal typealias RefreshKeyProvider<Param> = (suspend () -> Param?)
internal typealias AppendKeyProvider<Param> = (suspend () -> Param?)

internal typealias PagingSourceLoader<Param, Item>
        = (suspend (param: ItemPagingSource.LoadParams<Param>) -> ItemPagingSource.LoadResult<Param, Item>)