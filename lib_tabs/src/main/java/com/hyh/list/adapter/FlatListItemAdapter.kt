package com.hyh.list.adapter

import androidx.recyclerview.widget.RecyclerView
import com.hyh.InvokeWithParam
import com.hyh.coroutine.*
import com.hyh.coroutine.SingleRunner
import com.hyh.coroutine.simpleScan
import com.hyh.list.*
import com.hyh.list.internal.*
import com.hyh.page.PageContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 对应一个[ItemSource]
 *
 * @author eriche
 * @data 2021/6/7
 */
@Suppress("UNCHECKED_CAST")
class FlatListItemAdapter(
    pageContext: PageContext,
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

    private val _loadStateFlow: SimpleMutableStateFlow<SourceLoadState> = SimpleMutableStateFlow(SourceLoadState.Initial)
    val loadStateFlow: SimpleStateFlow<SourceLoadState>
        get() = _loadStateFlow.asStateFlow()

    private val resultFlow: SimpleMutableStateFlow<Pair<Long, SourceEvent?>> = SimpleMutableStateFlow<Pair<Long, SourceEvent?>>(Pair(0, null))

    init {
        pageContext.lifecycleScope.launch {
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
                            _loadStateFlow.value = SourceLoadState.Loading
                            onStateChanged(SourceLoadState.Loading)
                            event.onReceived()
                        }
                        is SourceEvent.PreShowing -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.RefreshSuccess -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.RefreshError -> {
                            _loadStateFlow.value = SourceLoadState.Error(event.error, event.preShowing)
                            onStateChanged(SourceLoadState.Error(event.error, event.preShowing))
                            event.onReceived()
                        }
                        is SourceEvent.ItemRemoved -> {
                            val processedResult = event.processor.invoke()
                            _items = processedResult.resultItems
                            processedResult.onResultUsed()
                            ListUpdate.handleListOperates(processedResult.listOperates, this@FlatListItemAdapter)
                            event.onReceived()
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        receiver?.accessItem(position)
    }

    fun refresh(important: Boolean) {
        receiver?.refresh(important)
    }

    fun destroy() {
        _loadStateFlow.close()
        resultFlow.close()
        _items?.forEach {
            it.delegate.onItemDetached()
        }
        receiver?.destroy()
    }

    inner class ResultProcessorSnapshot(private val sourceEvent: SourceEvent) {

        private val coroutineScope = CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        fun handleSourceEvent() {
            coroutineScope.launch {
                when (sourceEvent) {
                    is SourceEvent.PreShowing -> {
                        val processedResult = sourceEvent.processor.invoke()
                        _items = processedResult.resultItems
                        processedResult.onResultUsed()
                        ListUpdate.handleListOperates(processedResult.listOperates, this@FlatListItemAdapter)
                        _loadStateFlow.value = SourceLoadState.PreShow(processedResult.resultItems.size)
                        onStateChanged(SourceLoadState.PreShow(processedResult.resultItems.size))
                        sourceEvent.onReceived()
                    }
                    is SourceEvent.RefreshSuccess -> {
                        val processedResult = sourceEvent.processor.invoke()
                        _items = processedResult.resultItems
                        processedResult.onResultUsed()
                        ListUpdate.handleListOperates(processedResult.listOperates, this@FlatListItemAdapter)
                        _loadStateFlow.value = SourceLoadState.Success(processedResult.resultItems.size)
                        onStateChanged(SourceLoadState.Success(processedResult.resultItems.size))
                        sourceEvent.onReceived()
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