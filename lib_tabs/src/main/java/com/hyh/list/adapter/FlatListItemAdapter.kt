package com.hyh.list.adapter

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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


    private val resultFlow: SimpleMutableStateFlow<Pair<Long, SourceEvent?>> = SimpleMutableStateFlow(Pair(0, null))

    private var recyclerView: RecyclerView? = null


    init {
        lifecycleOwner.lifecycleScope.launch {
            resultFlow
                .asStateFlow()
                .map { it.second }
                .filterNotNull()
                .simpleScan(null) { previousSnapshot: ResultProcessorSnapshot?, sourceEvent: SourceEvent ->
                    previousSnapshot?.close()
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
                            _loadStateFlow.value = ItemSourceLoadState.Loading
                            onStateChanged(ItemSourceLoadState.Loading)
                            event.onReceived()
                        }
                        is SourceEvent.PreShowing -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.RefreshSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.RefreshError -> {
                            _loadStateFlow.value = ItemSourceLoadState.Error(event.error, event.preShowing, itemCount)
                            onStateChanged(ItemSourceLoadState.Error(event.error, event.preShowing, itemCount))
                            event.onReceived()
                        }


                        is SourceEvent.PagingRefreshing -> {
                            _pagingLoadStateFlow.value = PagingSourceLoadState.Refreshing
                            _loadStateFlow.value = ItemSourceLoadState.Loading
                            onStateChanged(PagingSourceLoadState.Refreshing)
                            event.onReceived()
                        }
                        is SourceEvent.PagingRefreshSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.PagingRefreshError -> {
                            val refreshError = PagingSourceLoadState.RefreshError(event.error)
                            _pagingLoadStateFlow.value = refreshError
                            _loadStateFlow.value = ItemSourceLoadState.Error(event.error, false, itemCount)
                            onStateChanged(refreshError)
                            event.onReceived()
                        }

                        is SourceEvent.PagingAppending -> {
                            _pagingLoadStateFlow.value = PagingSourceLoadState.Appending
                            onStateChanged(PagingSourceLoadState.Appending)
                            event.onReceived()
                        }
                        is SourceEvent.PagingAppendSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.PagingAppendError -> {
                            val appendError = PagingSourceLoadState.AppendError(event.error)
                            _pagingLoadStateFlow.value = appendError
                            onStateChanged(appendError)
                            event.onReceived()
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
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
        receiver?.move(from, to)
        return true
    }

    fun removeItem(position: Int, count: Int) {
        receiver?.removeItem(position, count)
    }

    fun removeItem(item: FlatListItem) {
        receiver?.removeItem(item)
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
                PagingSourceLoadState.RefreshSuccess(sourceEvent.endOfPaginationReached)
            }
            is SourceEvent.PagingAppendSuccess -> {
                PagingSourceLoadState.AppendSuccess(sourceEvent.endOfPaginationReached)
            }
            is SourceEvent.ItemOperate -> {
                null
            }
            else -> {
                null
            }
        }
    }

    inner class ResultProcessorSnapshot(private val sourceEvent: SourceEvent) {

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