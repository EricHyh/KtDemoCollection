package com.hyh.list.internal

import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import com.hyh.coroutine.SimpleMutableStateFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.list.FlatListItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*


abstract class BaseItemFetcher<Param : Any, Item : Any>(
    open val itemSource: BaseItemSource<Param, Item>
) {

    protected open val sourceDisplayedData = SourceDisplayedData<Item>()

    protected abstract val uiReceiver: BaseUiReceiverForSource

    val flow: Flow<SourceData> = simpleChannelFlow {
        launch {
            this@simpleChannelFlow.initChannelFlow()
        }
        launch {
            uiReceiver
                .eventFlow
                .flowOn(Dispatchers.Main)
                .map {
                    val processor = createSourceResultProcessor(it) ?: return@map null
                    val sourceDataFlow = flow {
                        emit(SourceEvent.ItemOperate(processor))
                    }
                    SourceData(sourceDataFlow, uiReceiver)
                }
                .filterNotNull()
                .collect {
                    send(it)
                }
        }
    }

    protected abstract suspend fun SendChannel<SourceData>.initChannelFlow()


    fun refresh(important: Boolean) {
        uiReceiver.refresh(important)
    }

    fun removeItem(item: FlatListItem) {
        uiReceiver.removeItem(item)
    }

    fun removeItem(position: Int) {
        uiReceiver.removeItem(position)
    }

    fun move(from: Int, to: Int) {
        uiReceiver.move(from, to)
    }

    protected fun getFetchDispatcherProvider(): DispatcherProvider<Param, Item> = ::getFetchDispatcher
    protected fun getProcessDataDispatcherProvider(): DispatcherProvider<Param, Item> = ::getProcessDataDispatcher
    private fun getFetchDispatcher(param: Param, displayedData: SourceDisplayedData<Item>): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param, displayedData)
    }

    private fun getProcessDataDispatcher(param: Param, displayedData: SourceDisplayedData<Item>): CoroutineDispatcher {
        return itemSource.getProcessDataDispatcher(param, displayedData)
    }

    private fun createSourceResultProcessor(event: OperateItemEvent): SourceResultProcessor? {
        return when (event) {
            is OperateItemEvent.RemoveItemByData -> {
                removeItemByDataProcessor(event)
            }
            is OperateItemEvent.RemoveItemByPosition -> {
                removeItemByPositionProcessor(event)
            }
            is OperateItemEvent.MoveItem -> {
                moveItemProcessor(event)
            }
            else -> null
        }
    }

    private fun removeItemByDataProcessor(event: OperateItemEvent.RemoveItemByData): SourceResultProcessor {
        val item = event.item
        val flatListItems = sourceDisplayedData.flatListItems
        val index = flatListItems?.indexOf(item) ?: -1
        return removeItemByPositionProcessor(OperateItemEvent.RemoveItemByPosition(index))
    }

    private fun removeItemByPositionProcessor(event: OperateItemEvent.RemoveItemByPosition): SourceResultProcessor {
        return run@{
            val position = event.position
            val flatListItems = sourceDisplayedData.flatListItems ?: emptyList()
            val index = if (position in flatListItems.indices) position else -1
            if (index < 0) {
                return@run SourceProcessedResult(flatListItems, emptyList()) {}
            } else {
                val newOriginalItems = sourceDisplayedData.originalItems?.toMutableList()
                val newFlatListItems = sourceDisplayedData.flatListItems?.toMutableList()
                if (newOriginalItems == null || newFlatListItems == null) {
                    return@run SourceProcessedResult(flatListItems, emptyList()) {}
                }
                if (index !in newOriginalItems.indices) {
                    return@run SourceProcessedResult(flatListItems, emptyList()) {}
                }
                val originalItem = newOriginalItems.removeAt(index)
                newFlatListItems.removeAt(index)

                itemSource.delegate.onProcessResult(
                    newOriginalItems,
                    sourceDisplayedData.resultExtra,
                    sourceDisplayedData
                )

                return@run SourceProcessedResult(
                    newFlatListItems,
                    listOf(ListOperate.OnRemoved(index, 1))
                ) {
                    sourceDisplayedData.originalItems = newOriginalItems
                    sourceDisplayedData.flatListItems = newFlatListItems

                    itemSource.delegate.run {
                        onItemsRecycled(listOf(originalItem))
                        onResultDisplayed(sourceDisplayedData)
                    }
                }
            }
        }
    }

    private fun moveItemProcessor(event: OperateItemEvent.MoveItem): SourceResultProcessor {
        return run@{
            val from = event.from
            val to = event.to
            val flatListItems = sourceDisplayedData.flatListItems ?: emptyList()

            if (flatListItems.isNotEmpty()
                && from in flatListItems.indices
                && to in flatListItems.indices
            ) {
                val newOriginalItems = sourceDisplayedData.originalItems?.toMutableList()
                val newFlatListItems = sourceDisplayedData.flatListItems?.toMutableList()
                if (newOriginalItems != null
                    && newFlatListItems != null
                    && newOriginalItems.size == newFlatListItems.size
                ) {
                    if (ListUpdate.move(newOriginalItems, from, to)
                        && ListUpdate.move(newFlatListItems, from, to)
                    ) {

                        itemSource.delegate.onProcessResult(
                            newOriginalItems,
                            sourceDisplayedData.resultExtra,
                            sourceDisplayedData
                        )

                        return@run SourceProcessedResult(
                            newFlatListItems,
                            listOf(ListOperate.OnMoved(from, to))
                        ) {
                            sourceDisplayedData.originalItems = newOriginalItems
                            sourceDisplayedData.flatListItems = newFlatListItems
                            itemSource.delegate.run {
                                onResultDisplayed(sourceDisplayedData)
                            }
                        }
                    }
                }
            }

            return@run SourceProcessedResult(flatListItems, emptyList()) {}
        }
    }
}


abstract class BaseUiReceiverForSource : UiReceiverForSource {

    private val eventState = SimpleMutableStateFlow<OperateItemEvent>(OperateItemEvent.Initial)

    val eventFlow = eventState.asStateFlow()

    override fun removeItem(item: FlatListItem) {
        eventState.value = OperateItemEvent.RemoveItemByData(item)
    }

    override fun removeItem(position: Int) {
        eventState.value = OperateItemEvent.RemoveItemByPosition(position)
    }

    override fun move(from: Int, to: Int) {
        eventState.value = OperateItemEvent.MoveItem(from, to)
    }

    @CallSuper
    override fun destroy() {
        eventState.close()
    }
}


sealed class OperateItemEvent {

    object Initial : OperateItemEvent()

    class RemoveItemByData(val item: FlatListItem) : OperateItemEvent()

    class RemoveItemByPosition(val position: Int) : OperateItemEvent()

    class MoveItem(val from: Int, val to: Int) : OperateItemEvent()

}

internal typealias DispatcherProvider<Param, Item> = ((Param, SourceDisplayedData<Item>) -> CoroutineDispatcher?)