package com.hyh.list.internal.base

import androidx.annotation.CallSuper
import com.hyh.coroutine.SimpleMutableStateFlow
import com.hyh.coroutine.simpleChannelFlow
import com.hyh.list.FlatListItem
import com.hyh.list.internal.*
import com.hyh.list.internal.utils.ListOperate
import com.hyh.list.internal.utils.ListUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*


abstract class BaseItemFetcher<Param, Item>(
    open val itemSource: BaseItemSource<Param, Item>
) {

    protected open val sourceDisplayedData = SourceDisplayedData()

    protected abstract val uiReceiver: BaseUiReceiverForSource

    val itemOperator: IItemOperator
        get() = uiReceiver

    val flow: Flow<SourceData> = simpleChannelFlow {
        initChannelFlow()
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

    fun removeItem(position: Int, count: Int = 1) {
        uiReceiver.removeItem(position, count)
    }

    fun moveItem(from: Int, to: Int) {
        uiReceiver.moveItem(from, to)
    }

    protected fun getFetchDispatcherProvider(): DispatcherProvider<Param> = ::getFetchDispatcher
    protected fun getProcessDataDispatcherProvider(): DispatcherProvider<Param> =
        ::getProcessDataDispatcher

    private fun getFetchDispatcher(
        param: Param,
        displayedData: SourceDisplayedData
    ): CoroutineDispatcher {
        return itemSource.getFetchDispatcher(param, displayedData)
    }

    private fun getProcessDataDispatcher(
        param: Param,
        displayedData: SourceDisplayedData
    ): CoroutineDispatcher {
        return itemSource.getProcessDataDispatcher(param, displayedData)
    }

    private fun createSourceResultProcessor(event: OperateItemEvent): SourceResultProcessor? {
        return when (event) {
            is OperateItemEvent.RemoveItem -> {
                removeItemProcessor(event)
            }
            is OperateItemEvent.RemoveItems -> {
                removeItemsProcessor(event)
            }
            is OperateItemEvent.MoveItem -> {
                moveItemProcessor(event)
            }
            else -> null
        }
    }

    private fun removeItemProcessor(event: OperateItemEvent.RemoveItem): SourceResultProcessor {
        val item = event.item
        val flatListItems = sourceDisplayedData.flatListItems
        val index = flatListItems?.indexOf(item) ?: -1
        return removeItemsProcessor(OperateItemEvent.RemoveItems(index, 1))
    }

    private fun removeItemsProcessor(event: OperateItemEvent.RemoveItems): SourceResultProcessor {
        return run@{
            val position = event.position
            val count = event.count
            val flatListItems = sourceDisplayedData.flatListItems ?: emptyList()
            val index = if (position in flatListItems.indices) position else -1

            if (index < 0) {
                return@run SourceProcessedResult(flatListItems, emptyList()) {}
            } else {
                val newFlatListItems = sourceDisplayedData.flatListItems?.toMutableList()
                    ?: return@run SourceProcessedResult(flatListItems, emptyList()) {}
                if (index !in newFlatListItems.indices) {
                    return@run SourceProcessedResult(flatListItems, emptyList()) {}
                }

                val removedItems = ListUpdate.remove(newFlatListItems, position, count)
                if (removedItems.isEmpty()) {
                    return@run SourceProcessedResult(flatListItems, emptyList()) {}
                }

                itemSource.delegate.onProcessResult(
                    newFlatListItems,
                    sourceDisplayedData.resultExtra,
                    sourceDisplayedData
                )

                return@run SourceProcessedResult(
                    newFlatListItems,
                    listOf(ListOperate.OnRemoved(index, count))
                ) {
                    sourceDisplayedData.flatListItems = newFlatListItems

                    itemSource.delegate.run {
                        onItemsRecycled(removedItems)
                        onResultDisplayed(sourceDisplayedData)
                    }
                }
            }
        }
    }

    private fun insertItemsProcessor(event: OperateItemEvent.InsertItems): SourceResultProcessor {
        return run@{
            val position = event.position
            val flatListItems = sourceDisplayedData.flatListItems ?: emptyList()
            if (position != 0 && position !in flatListItems.indices) {
                return@run SourceProcessedResult(flatListItems, emptyList()) {}
            }
            val newFlatListItems = flatListItems + event.items

            itemSource.delegate.onProcessResult(
                newFlatListItems,
                sourceDisplayedData.resultExtra,
                sourceDisplayedData
            )

            return@run SourceProcessedResult(
                newFlatListItems,
                listOf(ListOperate.OnInserted(position, event.items.size))
            ) {
                sourceDisplayedData.flatListItems = newFlatListItems

                itemSource.delegate.run {
                    onItemsDisplayed(event.items)
                    onResultDisplayed(sourceDisplayedData)
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
                val newFlatListItems = sourceDisplayedData.flatListItems?.toMutableList()
                if (newFlatListItems != null) {
                    if (ListUpdate.move(newFlatListItems, from, to)) {
                        itemSource.delegate.onProcessResult(
                            newFlatListItems,
                            sourceDisplayedData.resultExtra,
                            sourceDisplayedData
                        )

                        return@run SourceProcessedResult(
                            newFlatListItems,
                            listOf(ListOperate.OnMoved(from, to))
                        ) {
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

    override fun moveItem(from: Int, to: Int): Boolean {
        val indices = getDisplayedItems().indices
        if (from !in indices || to !in indices) {
            return false
        }
        eventState.value = OperateItemEvent.MoveItem(from, to)
        return true
    }

    override fun removeItem(item: FlatListItem) {
        eventState.value = OperateItemEvent.RemoveItem(item)
    }

    override fun removeItem(position: Int, count: Int) {
        eventState.value = OperateItemEvent.RemoveItems(position, count)
    }

    override fun insertItems(position: Int, items: List<FlatListItem>) {
        eventState.value = OperateItemEvent.InsertItems(position, items)
    }

    abstract fun getDisplayedItems(): List<FlatListItem>

    @CallSuper
    override fun destroy() {
        eventState.close()
    }
}


sealed class OperateItemEvent {

    object Initial : OperateItemEvent()

    class RemoveItem(val item: FlatListItem) : OperateItemEvent()

    class RemoveItems(val position: Int, val count: Int) : OperateItemEvent()

    class InsertItems(val position: Int, val items: List<FlatListItem>) : OperateItemEvent()

    class MoveItem(val from: Int, val to: Int) : OperateItemEvent()

}

internal typealias DispatcherProvider<Param> = ((Param, SourceDisplayedData) -> CoroutineDispatcher?)