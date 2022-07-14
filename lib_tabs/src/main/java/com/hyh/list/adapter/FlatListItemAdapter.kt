package com.hyh.list.adapter

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.InvokeWithParam
import com.hyh.coroutine.*
import com.hyh.coroutine.SingleRunner
import com.hyh.coroutine.simpleScan
import com.hyh.list.*
import com.hyh.list.internal.*
import com.hyh.list.internal.utils.ListUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 对应一个[ItemSource]
 *
 * @author eriche
 * @data 2021/6/7
 */
@Suppress("UNCHECKED_CAST")
class FlatListItemAdapter constructor(
    lifecycleOwner: LifecycleOwner,
    private val flatListManager: IFlatListManager,
    private val onStateChanged: InvokeWithParam<SourceLoadState>
) : BaseFlatListItemAdapter() {

    companion object {
        private const val TAG = "FlatListItemAdapter"
    }

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private var receiver: UiReceiverForSource? = null

    private var _items: List<FlatListItem>? = null
    val items: List<FlatListItem>?
        get() = _items

    private val _loadStateFlow: SimpleMutableStateFlow<ItemSourceLoadState> =
        SimpleMutableStateFlow(ItemSourceLoadState.Initial)
    val loadStateFlow: SimpleStateFlow<ItemSourceLoadState>
        get() = _loadStateFlow.asStateFlow()

    private val _pagingLoadStateFlow: SimpleMutableStateFlow<PagingSourceLoadState> =
        SimpleMutableStateFlow(PagingSourceLoadState.Initial)
    val pagingLoadStateFlow: SimpleStateFlow<PagingSourceLoadState>
        get() = _pagingLoadStateFlow.asStateFlow()


    private val resultFlow: SimpleMutableStateFlow<Pair<Long, SourceEvent?>> =
        SimpleMutableStateFlow(Pair(0, null))

    private var recyclerView: RecyclerView? = null

    private var pendingAction: Invoke? = null

    init {
        lifecycleOwner.lifecycleScope.launch {
            resultFlow
                .asStateFlow()
                .map { it.second }
                .filterNotNull()
                .simpleScan(null) { previousSnapshot: ResultProcessorSnapshot?, sourceEvent: SourceEvent ->
                    if ((previousSnapshot?.sourceEvent is SourceEvent.PagingRearrangeSuccess
                                && sourceEvent is SourceEvent.PagingRearrangeSuccess)
                        || (previousSnapshot?.sourceEvent !is SourceEvent.PagingRearrangeSuccess
                                && sourceEvent !is SourceEvent.PagingRearrangeSuccess)
                    ) {
                        previousSnapshot?.close()
                    }
                    ResultProcessorSnapshot(sourceEvent)
                }
                .filterNotNull()
                .collect {
                    it.handleSourceEvent()
                }
        }
    }

    override fun getFlatListItems(): List<FlatListItem>? {
        return items
    }

    suspend fun submitData(data: SourceData) {
        collectFromRunner.runInIsolation {
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is SourceEvent.Loading -> {
                            _loadStateFlow.value = ItemSourceLoadState.Loading(itemCount)
                            onStateChanged(ItemSourceLoadState.Loading(itemCount))
                            event.onReceived()
                        }
                        is SourceEvent.PreShowing -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.RefreshSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.RefreshError -> {
                            _loadStateFlow.value = ItemSourceLoadState.Error(itemCount, event.error, event.preShowing)
                            onStateChanged(ItemSourceLoadState.Error(itemCount, event.error, event.preShowing))
                            event.onReceived()
                        }


                        is SourceEvent.PagingRefreshing -> {
                            _pagingLoadStateFlow.value = PagingSourceLoadState.Refreshing(itemCount)
                            _loadStateFlow.value = ItemSourceLoadState.Loading(itemCount)
                            onStateChanged(PagingSourceLoadState.Refreshing(itemCount))
                            event.onReceived()
                        }
                        is SourceEvent.PagingRefreshSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.PagingRefreshError -> {
                            val refreshError = PagingSourceLoadState.RefreshError(itemCount, event.error)
                            _pagingLoadStateFlow.value = refreshError
                            _loadStateFlow.value = ItemSourceLoadState.Error(itemCount, event.error, false)
                            onStateChanged(refreshError)
                            event.onReceived()
                        }

                        is SourceEvent.PagingAppending -> {
                            _pagingLoadStateFlow.value = PagingSourceLoadState.Appending(itemCount)
                            onStateChanged(PagingSourceLoadState.Appending(itemCount))
                            event.onReceived()
                        }
                        is SourceEvent.PagingAppendSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.PagingAppendError -> {
                            val appendError = PagingSourceLoadState.AppendError(itemCount, event.error)
                            _pagingLoadStateFlow.value = appendError
                            onStateChanged(appendError)
                            event.onReceived()
                        }

                        is SourceEvent.PagingRearrangeSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }

                        is SourceEvent.ItemOperate -> {
                            processResult(event) {}
                        }

                        else -> {
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            setFlatListManager(flatListManager)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        recyclerView?.post {
            receiver?.accessItem(position)
        }
    }

    fun refresh(important: Boolean) {
        receiver?.refresh(important)
    }

    fun append(important: Boolean) {
        receiver?.append(important)
    }

    fun rearrange(important: Boolean) {
        receiver?.rearrange(important)
    }

    fun moveItem(from: Int, to: Int): Boolean {
        receiver ?: return false
        return receiver?.moveItem(from, to) == true
    }

    fun removeItem(position: Int, count: Int) {
        receiver?.removeItem(position, count)
    }

    fun removeItem(item: FlatListItem) {
        receiver?.removeItem(item)
    }

    fun insertItems(position: Int, items: List<FlatListItem>) {
        receiver?.insertItems(position, items)
    }

    fun requestKeepPosition(localInfo: ItemLocalInfo) {
        val recyclerView = recyclerView ?: return
        val linearLayoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val item = findItem(localInfo.localPosition) ?: return
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(localInfo.globalPosition)
        val top = viewHolder?.itemView?.top ?: 0

        pendingAction = invoke@{
            pendingAction = null
            var index: Int = -1
            if (getFlatListItems()?.any {
                    index++
                    item.areItemsTheSame(it)
                } == true) {

                linearLayoutManager.scrollToPositionWithOffset(localInfo.globalPosition, top)
            }
        }
    }

    fun destroy() {
        _loadStateFlow.close()
        resultFlow.close()
        receiver?.destroy()
    }

    private suspend fun processResult(
        sourceEvent: SourceEvent.ProcessorSourceEvent,
        onStateChanged: InvokeWithParam<SourceLoadState>
    ) {
        val processedResult = sourceEvent.processor.invoke()
        _items = processedResult.resultItems
        processedResult.onResultUsed()
        ListUpdate.handleListOperates(processedResult.listOperates, this@FlatListItemAdapter)

        pendingAction?.invoke()

        createSourceLoadState(sourceEvent, processedResult.resultItems)?.apply {
            onStateChanged(this)
        }
        sourceEvent.onReceived()
    }

    private fun createSourceLoadState(
        sourceEvent: SourceEvent.ProcessorSourceEvent,
        resultItems: List<FlatListItem>,
    ): SourceLoadState? {
        return when (sourceEvent) {
            is SourceEvent.PreShowing -> {
                ItemSourceLoadState.PreShow(resultItems.size)
            }
            is SourceEvent.RefreshSuccess -> {
                ItemSourceLoadState.Success(resultItems.size)
            }
            is SourceEvent.PagingRefreshSuccess -> {
                PagingSourceLoadState.RefreshSuccess(itemCount, sourceEvent.endOfPaginationReached)
            }
            is SourceEvent.PagingAppendSuccess -> {
                PagingSourceLoadState.AppendSuccess(itemCount, sourceEvent.endOfPaginationReached)
            }
            is SourceEvent.ItemOperate -> {
                null
            }
            else -> {
                null
            }
        }
    }

    inner class ResultProcessorSnapshot(val sourceEvent: SourceEvent) {

        private val coroutineScope =
            CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        fun handleSourceEvent() {
            coroutineScope.launch {
                when (sourceEvent) {
                    is SourceEvent.ProcessorSourceEvent -> {
                        processResult(sourceEvent) {
                            when (this) {
                                is ItemSourceLoadState -> {
                                    _loadStateFlow.value = this
                                }
                                is PagingSourceLoadState -> {
                                    _pagingLoadStateFlow.value = this
                                    if (sourceEvent is SourceEvent.PagingRefreshSuccess) {
                                        _loadStateFlow.value = ItemSourceLoadState.Success(
                                            _items?.size ?: 0
                                        )
                                    }
                                }
                            }
                            onStateChanged(this)
                        }
                    }
                    else -> {
                    }
                }
            }
        }

        fun close() {
            coroutineScope.close()
        }
    }
}