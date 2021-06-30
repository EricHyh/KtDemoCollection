package com.hyh.list.adapter

import com.hyh.coroutine.SingleRunner
import com.hyh.list.*
import com.hyh.list.internal.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * 对应一个[ItemSource]
 *
 * @author eriche
 * @data 2021/6/7
 */
@Suppress("UNCHECKED_CAST")
class SourceAdapter : ItemDataAdapter() {

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

    override fun getItemDataList(): List<ItemData>? {
        return items
    }

    suspend fun submitData(data: SourceData) {
        collectFromRunner.runInIsolation {
            resultFlow
                .asStateFlow()
                .map { it.second }
                .filterNotNull()
                .collectLatest {
                    when (it) {
                        is SourceEvent.PreShowing -> {
                            val processedResult = it.processor.invoke(itemWrappers, itemsBucketMap)
                            itemWrappers = processedResult.resultItemWrappers
                            itemsBucketMap = processedResult.resultItemsBucketMap
                            ListUpdate.handleListOperates(it.listOperates, this@SourceAdapter)
                            _loadStateFlow.value = SourceLoadState.PreShow(processedResult.resultItemWrappers.size)
                            it.onReceived()
                        }
                        is SourceEvent.Success -> {
                            val processedResult = it.processor.invoke(itemWrappers, itemsBucketMap)
                            itemWrappers = processedResult.resultItemWrappers
                            itemsBucketMap = processedResult.resultItemsBucketMap
                            ListUpdate.handleListOperates(it.listOperates, this@SourceAdapter)
                            _loadStateFlow.value = SourceLoadState.Success(processedResult.resultItemWrappers.size)
                            it.onReceived()
                        }
                        else -> {
                        }
                    }
                }
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
}