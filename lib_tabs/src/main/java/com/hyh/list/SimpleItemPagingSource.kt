package com.hyh.list

import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.utils.IElementDiff

/**
 * 支持加载更多的ItemSource
 *
 * @author eriche 2022/6/21
 */
abstract class SimpleItemPagingSource<Param>(initialParam: Param?) :
    ItemPagingSource<Param, FlatListItem>(initialParam) {

    override fun getElementDiff(): IElementDiff<FlatListItem> {
        return IElementDiff.ItemDataDiff()
    }

    override fun mapItems(items: List<FlatListItem>): List<FlatListItem> {
        return items
    }

    override fun onItemsChanged(changes: List<Triple<FlatListItem, FlatListItem, Any?>>) {
        changes.forEach {
            it.first.delegate.updateItem(it.second, it.third)
        }
    }

    override fun onItemsDisplayed(items: List<FlatListItem>) {
        items.forEach {
            if (!it.delegate.attached) {
                it.delegate.attachedSourceToken = sourceToken
                it.delegate.onItemAttached()
            }
            it.delegate.onItemActivated()
        }
    }

    override fun onItemsRecycled(items: List<FlatListItem>) {
        items.forEach {
            it.delegate.onItemInactivated()
            if (it.delegate.attached) {
                it.delegate.onItemDetached()
            }
        }
    }

    override fun areSourceContentsTheSame(newItemSource: BaseItemSource<LoadParams<Param>, FlatListItem>): Boolean {
        return false
    }

    override fun onUpdateItemSource(newItemSource: BaseItemSource<LoadParams<Param>, FlatListItem>) {
        super.onUpdateItemSource(newItemSource)
        if (newItemSource is SimpleItemPagingSource) {
            initialParam = newItemSource.initialParam
        }
    }

    override suspend fun getRefreshKey(): Param? {
        return initialParam
    }
}