package com.hyh.list.adapter

import android.util.Log
import com.hyh.coroutine.SingleRunner
import com.hyh.coroutine.simpleChannelFlow
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
class SourceAdapter(private val pageContext: PageContext) : ItemDataAdapter() {

    companion object {
        private const val TAG = "SourceAdapter"
    }

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private var receiver: UiReceiverForSource? = null

    private var _items: List<ItemData>? = null
    val items: List<ItemData>?
        get() = _items

    private var itemWrappers: List<ItemDataWrapper>? = null
    private var itemsBucketMap: Map<Int, ItemSource.ItemsBucket>? = null

    private val _loadStateFlow: MutableStateFlow<SourceLoadState> = MutableStateFlow(SourceLoadState.Initial)
    val loadStateFlow: StateFlow<SourceLoadState>
        get() = _loadStateFlow.asStateFlow()

    private val resultFlow: MutableStateFlow<Pair<Long, SourceEvent?>> = MutableStateFlow<Pair<Long, SourceEvent?>>(Pair(0, null))

    val flow: Flow<ProcessedResult> = simpleChannelFlow<ProcessedResult> {
        resultFlow
            .asStateFlow()
            .map { it.second }
            .filterNotNull()
            .simpleScan(null) { previousSnapshot: ResultProcessorSnapshot?, sourceEvent: SourceEvent ->
                ResultProcessorSnapshot()
            }
            .collect {
                /*when (it) {
                    is SourceEvent.PreShowing -> {
                        Log.d(TAG, "submitData: SourceEvent PreShowing")
                        val processedResult = it.processor.invoke(itemWrappers, itemsBucketMap)
                        _items = processedResult.resultItems
                        itemWrappers = processedResult.resultItemWrappers
                        itemsBucketMap = processedResult.resultItemsBucketMap
                        ListUpdate.handleListOperates(processedResult.listOperates, this@SourceAdapter)
                        _loadStateFlow.value = SourceLoadState.PreShow(processedResult.resultItemWrappers.size)
                        processedResult.onResultUsed()
                        it.onReceived()
                    }
                    is SourceEvent.Success -> {
                        Log.d(TAG, "submitData: SourceEvent Success")
                        val processedResult = it.processor.invoke(itemWrappers, itemsBucketMap)
                        _items = processedResult.resultItems
                        itemWrappers = processedResult.resultItemWrappers
                        itemsBucketMap = processedResult.resultItemsBucketMap
                        ListUpdate.handleListOperates(processedResult.listOperates, this@SourceAdapter)
                        _loadStateFlow.value = SourceLoadState.Success(processedResult.resultItemWrappers.size)
                        processedResult.onResultUsed()
                        it.onReceived()
                    }
                    else -> {
                    }
                }*/
            }
    }

    init {


    }

    override fun getItemDataList(): List<ItemData>? {
        return items
    }

    suspend fun submitData(data: SourceData) {
        collectFromRunner.runInIsolation {
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is SourceEvent.PreShowing -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                        is SourceEvent.Loading -> {
                            _loadStateFlow.value = SourceLoadState.Loading
                            event.onReceived()
                        }
                        is SourceEvent.Error -> {
                            _loadStateFlow.value = SourceLoadState.Error(event.error, event.preShowing)
                            event.onReceived()
                        }
                        is SourceEvent.Success -> {
                            resultFlow.value = Pair(resultFlow.value.first + 1, event)
                        }
                    }
                }
            }
        }
    }

    fun refresh(important: Boolean) {
        receiver?.refresh(important)
    }

    fun destroy() {
        _items?.forEach {
            it.delegate.onDetached()
        }
    }

    class ResultProcessorSnapshot {

    }
}