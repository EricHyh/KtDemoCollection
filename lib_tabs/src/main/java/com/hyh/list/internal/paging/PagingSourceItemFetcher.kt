package com.hyh.list.internal.paging

import com.hyh.coroutine.simpleMapLatest
import com.hyh.coroutine.simpleScan
import com.hyh.list.FlatListItem
import com.hyh.list.ItemPagingSource
import com.hyh.list.internal.*
import com.hyh.list.internal.base.BaseItemFetcher
import com.hyh.list.internal.base.BaseUiReceiverForSource
import com.hyh.list.internal.base.IItemFetcherSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*

class PagingSourceItemFetcher<Param, Item>(
    private val pagingSource: ItemPagingSource<Param, Item>
) : BaseItemFetcher<ItemPagingSource.LoadParams<Param>, Item>(pagingSource) {

    override val sourceDisplayedData: PagingSourceDisplayedData<Param> =
        PagingSourceDisplayedData()

    inner class ItemFetcherUiReceiver : BaseUiReceiverForSource() {

        private val loadEventHandler = LoadEventHandler()

        val flow = loadEventHandler.flow

        override fun refresh(important: Boolean) {
            loadEventHandler.onReceiveLoadEvent(LoadEvent.Refresh, important)
        }

        override fun append(important: Boolean) {
            loadEventHandler.onReceiveLoadEvent(LoadEvent.Append, important)
        }

        override fun rearrange(important: Boolean) {
            loadEventHandler.onReceiveLoadEvent(LoadEvent.Rearrange, important)
        }

        override fun accessItem(position: Int) {
            if (sourceDisplayedData.noMore) return
            if (sourceDisplayedData.lastPaging == null) return
            val size = sourceDisplayedData.flatListItems?.size ?: 0
            if (position >= size - 4) {
                append(false)
            }
        }

        fun onRefreshComplete() {
            loadEventHandler.onLoadEventComplete(LoadEvent.Refresh)
        }

        fun onAppendComplete() {
            loadEventHandler.onLoadEventComplete(LoadEvent.Append)
        }

        fun onRearrangeComplete() {
            loadEventHandler.onLoadEventComplete(LoadEvent.Rearrange)
        }

        override fun getDisplayedItems(): List<FlatListItem> {
            return sourceDisplayedData.flatListItems ?: emptyList()
        }
    }

    override val uiReceiver: ItemFetcherUiReceiver = ItemFetcherUiReceiver()

    override suspend fun SendChannel<SourceData>.initChannelFlow() {
        coroutineScope {
            launch {
                uiReceiver
                    .flow
                    .flowOn(Dispatchers.Main)
                    .simpleScan(null) { previousSnapshot: IItemFetcherSnapshot?, loadEvent: LoadEvent ->
                        if (loadEvent == LoadEvent.Rearrange) {
                            if (previousSnapshot is PagingSourceItemRearrangeSnapshot<*, *>
                                || (previousSnapshot is PagingSourceItemFetcherSnapshot<*, *> && !previousSnapshot.isRefresh)
                            ) {
                                previousSnapshot.close()
                            }
                            PagingSourceItemRearrangeSnapshot(
                                displayedData = sourceDisplayedData,
                                loader = getPagingSourceLoader(),
                                onRearrangeComplete = uiReceiver::onRearrangeComplete,
                                delegate = pagingSource.delegate,
                                fetchDispatcherProvider = getFetchDispatcherProvider(),
                                processDataDispatcherProvider = getProcessDataDispatcherProvider(),
                            )

                        } else {
                            if (sourceDisplayedData.noMore && loadEvent != LoadEvent.Refresh) {
                                uiReceiver.onAppendComplete()
                                return@simpleScan null
                            }
                            previousSnapshot?.close()
                            PagingSourceItemFetcherSnapshot(
                                displayedData = sourceDisplayedData,
                                refreshKeyProvider = getRefreshKeyProvider(),
                                appendKeyProvider = getAppendKeyProvider(),
                                loader = getPagingSourceLoader(),
                                onRefreshComplete = uiReceiver::onRefreshComplete,
                                onAppendComplete = uiReceiver::onAppendComplete,
                                delegate = pagingSource.delegate,
                                fetchDispatcherProvider = getFetchDispatcherProvider(),
                                processDataDispatcherProvider = getProcessDataDispatcherProvider(),
                                forceRefresh = loadEvent == LoadEvent.Refresh
                            )
                        }
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


    fun append(important: Boolean) {
        uiReceiver.append(important)
    }

    fun rearrange(important: Boolean) {
        uiReceiver.rearrange(important)
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


internal typealias RefreshKeyProvider<Param> = (suspend () -> Param?)
internal typealias AppendKeyProvider<Param> = (suspend () -> Param?)

internal typealias PagingSourceLoader<Param, Item>
        = (suspend (param: ItemPagingSource.LoadParams<Param>) -> ItemPagingSource.LoadResult<Param, Item>)