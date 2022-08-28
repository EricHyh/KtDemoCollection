package com.hyh.list.internal.paging

import android.util.Log
import com.hyh.Invoke
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.list.ItemPagingSource
import com.hyh.list.internal.*
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.base.DispatcherProvider
import com.hyh.list.internal.base.IItemFetcherSnapshot
import com.hyh.list.internal.utils.ListOperate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PagingSourceItemFetcherSnapshot<Param, Item> constructor(
    private val displayedData: PagingSourceDisplayedData<Param>,
    private val refreshKeyProvider: RefreshKeyProvider<Param>,
    private val appendKeyProvider: AppendKeyProvider<Param>,
    private val loader: PagingSourceLoader<Param, Item>,
    private val onRefreshComplete: Invoke,
    private val onAppendComplete: Invoke,
    private val delegate: BaseItemSource.Delegate<ItemPagingSource.LoadParams<Param>, Item>,
    private val fetchDispatcherProvider: DispatcherProvider<ItemPagingSource.LoadParams<Param>>,
    private val processDataDispatcherProvider: DispatcherProvider<ItemPagingSource.LoadParams<Param>>,
    private val forceRefresh: Boolean
) : IItemFetcherSnapshot {

    companion object {
        private const val TAG = "PagingSourceItemFetcher"
    }

    @Volatile
    private var closed = false
    private val sourceEventChannelFlowJob = Job()
    private val sourceEventCh = Channel<SourceEvent>(Channel.BUFFERED)

    @Volatile
    var isRefresh = false

    override val sourceEventFlow: Flow<SourceEvent> =
        cancelableChannelFlow(sourceEventChannelFlowJob) {
            val isRefresh = displayedData.lastPaging == null || forceRefresh
            this@PagingSourceItemFetcherSnapshot.isRefresh = isRefresh
            if (displayedData.noMore && !isRefresh) {
                onAppendComplete()
                return@cancelableChannelFlow
            }

            launch {
                sourceEventCh.consumeAsFlow().collect {
                    try {
                        if (closed) return@collect
                        send(it)
                    } catch (e: ClosedSendChannelException) {
                    }
                }
            }


            val param = if (isRefresh) {
                sourceEventCh.send(SourceEvent.PagingRefreshing())
                ItemPagingSource.LoadParams.Refresh(refreshKeyProvider.invoke())
            } else {
                sourceEventCh.send(SourceEvent.PagingAppending())
                ItemPagingSource.LoadParams.Append(appendKeyProvider.invoke())
            }

            val fetchDispatcher = fetchDispatcherProvider.invoke(param, displayedData)

            val loadResult: ItemPagingSource.LoadResult<Param, Item> =
                if (fetchDispatcher == null) {
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
                        SourceEvent.PagingAppendError(
                            loadResult.throwable,
                            displayedData.pagingSize
                        ) {
                            onAppendComplete()
                        }
                    }.apply {
                        sourceEventCh.send(this)
                    }
                }
                is ItemPagingSource.LoadResult.Success -> {
                    if (isRefresh) {
                        SourceEvent.PagingRefreshSuccess(
                            refreshProcessor(param, loadResult),
                            loadResult.noMore
                        ) {
                            onRefreshComplete()
                        }
                    } else {
                        SourceEvent.PagingAppendSuccess(
                            appendProcessor(param, loadResult),
                            displayedData.pagingSize,
                            loadResult.noMore
                        ) {
                            onAppendComplete()
                        }
                    }.apply {
                        sourceEventCh.send(this)
                    }
                }
                else -> {
                    throw IllegalArgumentException("loadResult shouldn't be $loadResult")
                }
            }
        }

    override fun close() {
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
            val resultExtra = success.resultExtra
            val flatListItems = delegate.mapItems(items)
            val nextParam = success.nextParam
            val noMore = success.noMore
            val onResultDisplayed = success.onResultDisplayed

            delegate.onProcessResult(
                flatListItems,
                resultExtra,
                displayedData
            )

            return SourceProcessedResult(flatListItems, listOf(ListOperate.OnAllChanged)) {
                val oldItems = displayedData.flatListItems
                displayedData.flatListItems = flatListItems
                displayedData.resultExtra = resultExtra
                displayedData.pagingList = listOf(
                    Paging(
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
                    if (oldItems?.isNotEmpty() == true) {
                        onItemsRecycled(oldItems)
                    }
                    onItemsDisplayed(flatListItems)
                }

                delegate.onResultDisplayed(displayedData)

                onResultDisplayed?.invoke()
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
            val resultExtra = success.resultExtra
            val flatListItems = delegate.mapItems(items)
            val nextParam = success.nextParam
            val noMore = success.noMore
            val onResultDisplayed = success.onResultDisplayed

            val oldFlatListItems = displayedData.flatListItems ?: emptyList()

            val resultFlatListItems = oldFlatListItems + flatListItems

            delegate.onProcessResult(
                resultFlatListItems,
                resultExtra,
                displayedData
            )

            return SourceProcessedResult(
                resultFlatListItems,
                listOf(ListOperate.OnInserted(oldFlatListItems.size, flatListItems.size))
            ) {
                displayedData.flatListItems = resultFlatListItems
                displayedData.resultExtra = resultExtra

                val pagingList = displayedData.pagingList

                displayedData.pagingList = pagingList + Paging(
                    flatListItems = flatListItems,
                    param = param.param,
                    nextParam = nextParam,
                    noMore = noMore
                )

                flatListItems.forEach {
                    it.delegate.bindParentLifecycle(delegate.lifecycleOwner.lifecycle)
                    it.delegate.displayedItems = resultFlatListItems
                }

                delegate.run {
                    onItemsDisplayed(flatListItems)
                }

                delegate.onResultDisplayed(displayedData)

                onResultDisplayed?.invoke()
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
        return !displayedData.flatListItems.isNullOrEmpty() && items.isNotEmpty()
    }
}