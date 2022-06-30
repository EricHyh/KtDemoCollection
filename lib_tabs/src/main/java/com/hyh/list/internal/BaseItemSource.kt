package com.hyh.list.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.hyh.RefreshActuator
import com.hyh.lifecycle.ChildLifecycleOwner
import com.hyh.lifecycle.IChildLifecycleOwner
import com.hyh.list.FlatListItem
import com.hyh.list.IFlatListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * [com.hyh.list.IFlatListItem]列表的数据源
 *
 * @param Param 参数泛型
 * @param Item 原始的 Item 数据泛型
 */
abstract class BaseItemSource<Param : Any, Item : Any> : LifecycleOwner {

    internal val delegate: Delegate<Param, Item> = object : Delegate<Param, Item>() {

        override val lifecycleOwner: ChildLifecycleOwner = ChildLifecycleOwner()

        private var displayedData: SourceDisplayedData? = null

        override val displayedFlatListItemsSnapshot: List<FlatListItem>?
            get() = displayedData?.flatListItems

        override var sourcePosition: Int = -1
            set(value) {
                val oldPosition = field
                field = value
                if (oldPosition != value) {
                    onSourcePositionChanged(oldPosition, value)
                }
            }

        override fun attach() {
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
            this@BaseItemSource.onAttached()
        }

        override fun injectRefreshActuator(refreshActuator: RefreshActuator) {
            _refreshActuator = refreshActuator
        }

        override fun getElementDiff(): IElementDiff<FlatListItem> {
            return this@BaseItemSource.getElementDiff()
        }

        override fun mapItems(items: List<Item>): List<FlatListItem> {
            return this@BaseItemSource.mapItems(items)
        }

        override fun areSourceTheSame(newItemSource: BaseItemSource<Param, Item>): Boolean {
            return this@BaseItemSource.areSourceTheSame(newItemSource)
        }

        override fun updateItemSource(newItemSource: BaseItemSource<Param, Item>) {
            onUpdateItemSource(newItemSource)
        }

        override fun onItemsDisplayed(items: List<FlatListItem>) {
            return this@BaseItemSource.onItemsDisplayed(items)
        }

        override fun onItemsChanged(changes: List<Triple<FlatListItem, FlatListItem, Any?>>) {
            return this@BaseItemSource.onItemsChanged(changes)
        }

        override fun onItemsRecycled(items: List<FlatListItem>) {
            return this@BaseItemSource.onItemsRecycled(items)
        }

        override fun onProcessResult(
            resultItems: List<FlatListItem>,
            resultExtra: Any?,
            displayedData: SourceDisplayedData
        ) {
            return this@BaseItemSource.onProcessResult(resultItems, resultExtra, displayedData)
        }

        override fun onResultDisplayed(displayedData: SourceDisplayedData) {
            this.displayedData = displayedData
            return this@BaseItemSource.onResultDisplayed(displayedData)
        }

        override fun detach() {
            displayedData?.let { displayedData ->
                displayedData.flatListItems?.forEach { item ->
                    item.delegate.onItemInactivated()
                    item.delegate.onItemDetached()
                }
            }
            displayedData = null
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED
            this@BaseItemSource.onDetached()
        }
    }

    protected val displayedFlatListItemsSnapshot: List<FlatListItem>?
        get() = delegate.displayedFlatListItemsSnapshot

    abstract val sourceToken: Any

    val sourcePosition: Int
        get() = delegate.sourcePosition

    private lateinit var _refreshActuator: RefreshActuator
    val refreshActuator: RefreshActuator
        get() = _refreshActuator


    final override fun getLifecycle(): Lifecycle {
        return delegate.lifecycleOwner.lifecycle
    }

    protected open fun onAttached() {}
    protected open fun onSourcePositionChanged(oldPosition: Int, newPosition: Int) {}

    protected open fun areSourceTheSame(newItemSource: BaseItemSource<Param, Item>): Boolean {
        return this.javaClass == newItemSource.javaClass
    }

    protected open fun onUpdateItemSource(newItemSource: BaseItemSource<Param, Item>) {}


    protected abstract fun getElementDiff(): IElementDiff<FlatListItem>
    protected abstract fun mapItems(items: List<Item>): List<FlatListItem>
    protected abstract fun onItemsDisplayed(items: List<FlatListItem>)
    protected abstract fun onItemsChanged(changes: List<Triple<FlatListItem, FlatListItem, Any?>>)
    protected abstract fun onItemsRecycled(items: List<FlatListItem>)


    protected open fun onProcessResult(
        resultItems: List<FlatListItem>,
        resultExtra: Any?,
        displayedData: SourceDisplayedData
    ) {
    }

    protected open fun onResultDisplayed(displayedData: SourceDisplayedData) {}

    open fun getFetchDispatcher(
        param: Param,
        displayedData: SourceDisplayedData
    ): CoroutineDispatcher =
        Dispatchers.Unconfined

    open fun getProcessDataDispatcher(
        param: Param,
        displayedData: SourceDisplayedData
    ): CoroutineDispatcher =
        getFetchDispatcher(param, displayedData)

    protected open fun onDetached() {}

    abstract class Delegate<Param : Any, Item : Any> : IChildLifecycleOwner {

        abstract val displayedFlatListItemsSnapshot: List<FlatListItem>?

        abstract var sourcePosition: Int

        abstract fun attach()

        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator)

        abstract fun getElementDiff(): IElementDiff<FlatListItem>
        abstract fun mapItems(items: List<Item>): List<FlatListItem>

        abstract fun areSourceTheSame(newItemSource: BaseItemSource<Param, Item>): Boolean
        abstract fun updateItemSource(newItemSource: BaseItemSource<Param, Item>)

        abstract fun onItemsDisplayed(items: List<FlatListItem>)
        abstract fun onItemsChanged(changes: List<Triple<FlatListItem, FlatListItem, Any?>>)
        abstract fun onItemsRecycled(items: List<FlatListItem>)

        abstract fun onProcessResult(
            resultItems: List<FlatListItem>,
            resultExtra: Any?,
            displayedData: SourceDisplayedData
        )

        abstract fun onResultDisplayed(displayedData: SourceDisplayedData)

        abstract fun detach()

    }

}