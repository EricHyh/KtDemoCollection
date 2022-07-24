package com.hyh.list

import com.hyh.base.RefreshStrategy
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.utils.IElementDiff

/**
 * 实现了只在还未展示数据时，获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleItemSource<Param>(protected var initialParam: Param) :
    ItemSource<Param, FlatListItem>() {

    final override fun getElementDiff(): IElementDiff<FlatListItem> {
        return IElementDiff.ItemDataDiff()
    }

    final override fun mapItems(items: List<FlatListItem>): List<FlatListItem> {
        return items
    }

    override fun getRefreshStrategy(): RefreshStrategy {
        if (displayedFlatListItemsSnapshot == null) return RefreshStrategy.QueueUp
        return super.getRefreshStrategy()
    }

    final override fun onItemsDisplayed(items: List<FlatListItem>) {
        items.forEach {
            if (!it.delegate.attached) {
                it.delegate.attachedSourceToken = sourceToken
                it.delegate.onItemAttached()
            }
            it.delegate.onItemActivated()
        }
    }

    final override fun onItemsChanged(changes: List<Triple<FlatListItem, FlatListItem, Any?>>) {
        changes.forEach {
            it.first.delegate.updateItem(it.second, it.third)
        }
    }

    final override fun onItemsRecycled(items: List<FlatListItem>) {
        items.forEach {
            it.delegate.onItemInactivated()
            if (it.delegate.attached) {
                it.delegate.onItemDetached()
            }
        }
    }


    override fun areSourceContentsTheSame(newItemSource: BaseItemSource<Param, FlatListItem>): Boolean {
        return false
    }

    override fun onUpdateItemSource(newItemSource: BaseItemSource<Param, FlatListItem>) {
        super.onUpdateItemSource(newItemSource)
        if (newItemSource is SimpleItemSource) {
            initialParam = newItemSource.initialParam
        }
    }

    override suspend fun getParam(): Param {
        return initialParam
    }

    override suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult<FlatListItem> {
        return if (params.displayedData.flatListItems == null) {
            getPreShow(params.param)
        } else {
            PreShowResult.Unused()
        }
    }

    override suspend fun load(params: LoadParams<Param>): LoadResult<FlatListItem> {
        return load(params.param)
    }

    /**
     * 列表中没数据的时候会调用该函数.
     *
     * 场景1：可以在这里获取缓存中的数据
     *
     * @param param
     * @return
     */
    protected abstract suspend fun getPreShow(param: Param): PreShowResult<FlatListItem>

    /**
     * 每次刷新的时候会执行该函数，执行的线程由[getFetchDispatcher]决定，默认是在主线程中执行
     *
     * @param param
     * @return
     */
    protected abstract suspend fun load(param: Param): LoadResult<FlatListItem>

}