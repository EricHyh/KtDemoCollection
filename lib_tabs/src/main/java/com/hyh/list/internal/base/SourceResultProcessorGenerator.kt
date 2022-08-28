package com.hyh.list.internal.base

import com.hyh.Invoke
import com.hyh.list.internal.*
import com.hyh.list.internal.utils.ListUpdate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SourceResultProcessorGenerator<Param, Item> constructor(
    private val sourceDisplayedData: SourceDisplayedData,
    private val items: List<Item>,
    private val resultExtra: Any?,
    private val onResultDisplayed: Invoke?,
    private val dispatcher: CoroutineDispatcher?,
    private val delegate: BaseItemSource.Delegate<Param, Item>
) {

    val processor: SourceResultProcessor = {
        if (shouldUseDispatcher()) {
            withContext(dispatcher!!) {
                processResult()
            }
        } else {
            processResult()
        }
    }

    private fun shouldUseDispatcher(): Boolean {
        return (dispatcher != null
                && !sourceDisplayedData.flatListItems.isNullOrEmpty()
                && items.isNotEmpty())
    }

    private fun processResult(): SourceProcessedResult {

        val newItems = delegate.mapItems(items)

        val updateResult = ListUpdate.calculateDiff(
            sourceDisplayedData.flatListItems,
            newItems,
            delegate.getElementDiff()
        )

        val flatListItems = updateResult.resultList

        delegate.onProcessResult(
            updateResult.resultList,
            resultExtra,
            sourceDisplayedData
        )

        return SourceProcessedResult(flatListItems, updateResult.listOperates) {
            sourceDisplayedData.flatListItems = flatListItems
            sourceDisplayedData.resultExtra = resultExtra

            flatListItems.forEach {
                it.delegate.bindParentLifecycle(delegate.lifecycleOwner.lifecycle)
                it.delegate.displayedItems = flatListItems
            }

            delegate.run {
                onItemsRecycled(updateResult.elementOperates.removedElements)
                onItemsChanged(updateResult.elementOperates.changedElements)
                onItemsDisplayed(updateResult.elementOperates.addedElements)
            }

            delegate.onResultDisplayed(sourceDisplayedData)

            onResultDisplayed?.invoke()
        }
    }
}