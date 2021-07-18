package com.hyh.list

import com.hyh.list.internal.IElementDiff

/**
 * 实现了只在还未展示数据时，获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleItemSource<Param : Any> : ItemSource<Param, ItemData>() {

    final override fun getElementDiff(): IElementDiff<ItemData> {
        return IElementDiff.ItemDataDiff()
    }

    final override fun mapItems(items: List<ItemData>): List<ItemData> {
        return items
    }

    final override fun onItemsDisplayed(items: List<ItemData>) {
        items.forEach {
            if (!it.delegate.attached) {
                it.delegate.onDataAttached()
            }
            it.delegate.onDataActivated()
        }
    }

    final override fun onItemsChanged(changes: List<Triple<ItemData, ItemData, Any?>>) {
        changes.forEach {
            it.first.delegate.updateItemData(it.second, it.third)
        }
    }

    final override fun onItemsRecycled(items: List<ItemData>) {
        items.forEach {
            it.delegate.onDataInactivated()
            if (it.delegate.attached) {
                it.delegate.onDataDetached()
            }
        }
    }

    override suspend fun getPreShow(params: PreShowParams<Param, ItemData>): PreShowResult<ItemData> {
        return if (params.displayedData.items == null) {
            getPreShow(params.param)
        } else {
            PreShowResult.Unused()
        }
    }

    override suspend fun load(params: LoadParams<Param, ItemData>): LoadResult<ItemData> {
        return load(params.param)
    }

    protected abstract suspend fun getPreShow(param: Param): PreShowResult<ItemData>
    protected abstract suspend fun load(param: Param): LoadResult<ItemData>

}