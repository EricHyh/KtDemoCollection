package com.hyh.list

import com.hyh.RefreshActuator
import com.hyh.base.RefreshStrategy
import com.hyh.list.internal.IElementDiff
import com.hyh.list.internal.SourceDisplayedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * [IFlatListItem]列表的数据源
 *
 * @param Param 参数泛型
 * @param Item 原始的 Item 数据泛型
 */
abstract class ItemSource<Param : Any, Item : Any> {

    internal val delegate: Delegate<Param, Item> = object : Delegate<Param, Item>() {

        private var displayedData: SourceDisplayedData<Item>? = null

        override val displayedOriginalItemsSnapshot: List<Item>?
            get() = displayedData?.originalItems

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
            this@ItemSource.onAttached()
        }

        override fun injectRefreshActuator(refreshActuator: RefreshActuator) {
            _refreshActuator = refreshActuator
        }

        override fun getElementDiff(): IElementDiff<Item> {
            return this@ItemSource.getElementDiff()
        }

        override fun mapItems(items: List<Item>): List<FlatListItem> {
            return this@ItemSource.mapItems(items)
        }

        override fun updateItemSource(newItemSource: ItemSource<Param, Item>) {
            onUpdateItemSource(newItemSource)
        }

        override fun onItemsDisplayed(items: List<Item>) {
            return this@ItemSource.onItemsDisplayed(items)
        }

        override fun onItemsChanged(changes: List<Triple<Item, Item, Any?>>) {
            return this@ItemSource.onItemsChanged(changes)
        }

        override fun onItemsRecycled(items: List<Item>) {
            return this@ItemSource.onItemsRecycled(items)
        }

        override fun onProcessResult(
            resultItems: List<Item>,
            resultExtra: Any?,
            displayedData: SourceDisplayedData<Item>
        ) {
            return this@ItemSource.onProcessResult(resultItems, resultExtra, displayedData)
        }

        override fun onResultDisplayed(displayedData: SourceDisplayedData<Item>) {
            this.displayedData = displayedData
            return this@ItemSource.onResultDisplayed(displayedData)
        }

        override fun detach() {
            this@ItemSource.onDetached()
        }
    }

    protected val displayedOriginalItemsSnapshot: List<Item>?
        get() = delegate.displayedOriginalItemsSnapshot

    protected val displayedFlatListItemsSnapshot: List<FlatListItem>?
        get() = delegate.displayedFlatListItemsSnapshot

    abstract val sourceToken: Any

    val sourcePosition: Int
        get() = delegate.sourcePosition

    private lateinit var _refreshActuator: RefreshActuator
    val refreshActuator: RefreshActuator
        get() = _refreshActuator

    protected open fun onAttached() {}

    protected open fun onSourcePositionChanged(oldPosition: Int, newPosition: Int) {}
    protected open fun onUpdateItemSource(newItemSource: ItemSource<Param, Item>) {}

    protected abstract fun getElementDiff(): IElementDiff<Item>
    protected abstract fun mapItems(items: List<Item>): List<FlatListItem>
    protected abstract fun onItemsDisplayed(items: List<Item>)
    protected abstract fun onItemsChanged(changes: List<Triple<Item, Item, Any?>>)
    protected abstract fun onItemsRecycled(items: List<Item>)

    open fun getRefreshStrategy(): RefreshStrategy = RefreshStrategy.CancelLast
    abstract suspend fun getParam(): Param
    abstract suspend fun getPreShow(params: PreShowParams<Param, Item>): PreShowResult<Item>
    abstract suspend fun load(params: LoadParams<Param, Item>): LoadResult<Item>

    protected open fun onProcessResult(
        resultItems: List<Item>,
        resultExtra: Any?,
        displayedData: SourceDisplayedData<Item>
    ) {
    }

    protected open fun onResultDisplayed(displayedData: SourceDisplayedData<Item>) {}

    open fun getFetchDispatcher(param: Param, displayedData: SourceDisplayedData<Item>): CoroutineDispatcher =
        Dispatchers.Unconfined

    open fun getProcessDataDispatcher(param: Param, displayedData: SourceDisplayedData<Item>): CoroutineDispatcher =
        getFetchDispatcher(param, displayedData)

    protected open fun onDetached() {}

    abstract class Delegate<Param : Any, Item : Any> {

        abstract val displayedOriginalItemsSnapshot: List<Item>?

        abstract val displayedFlatListItemsSnapshot: List<FlatListItem>?

        abstract var sourcePosition: Int

        abstract fun attach()

        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator)

        abstract fun getElementDiff(): IElementDiff<Item>
        abstract fun mapItems(items: List<Item>): List<FlatListItem>

        abstract fun updateItemSource(newItemSource: ItemSource<Param, Item>)

        abstract fun onItemsDisplayed(items: List<Item>)
        abstract fun onItemsChanged(changes: List<Triple<Item, Item, Any?>>)
        abstract fun onItemsRecycled(items: List<Item>)

        abstract fun onProcessResult(
            resultItems: List<Item>,
            resultExtra: Any?,
            displayedData: SourceDisplayedData<Item>
        )

        abstract fun onResultDisplayed(displayedData: SourceDisplayedData<Item>)

        abstract fun detach()

    }

    sealed class PreShowResult<Item : Any> {

        class Unused<Item : Any> : PreShowResult<Item>()

        class Success<Item : Any> private constructor() : PreShowResult<Item>() {

            private lateinit var _items: List<Item>
            val items: List<Item>
                get() = _items


            private var _resultExtra: Any? = null
            val resultExtra: Any?
                get() = _resultExtra

            constructor(items: List<Item>, resultExtra: Any? = null) : this() {
                this._items = items
                this._resultExtra = resultExtra
            }
        }
    }

    sealed class LoadResult<Item : Any> {

        class Error<Item : Any>(
            val error: Throwable
        ) : LoadResult<Item>()

        class Success<Item : Any> private constructor() : LoadResult<Item>() {

            private lateinit var _items: List<Item>
            val items: List<Item>
                get() = _items

            private var _resultExtra: Any? = null
            val resultExtra: Any?
                get() = _resultExtra

            constructor(items: List<Item>, resultExtra: Any? = null) : this() {
                this._items = items
                this._resultExtra = resultExtra
            }
        }
    }

    class PreShowParams<Param : Any, Item : Any>(
        val param: Param,
        val displayedData: SourceDisplayedData<Item>
    )

    class LoadParams<Param : Any, Item : Any>(
        val param: Param,
        val displayedData: SourceDisplayedData<Item>
    )
}