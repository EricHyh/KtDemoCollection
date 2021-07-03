package com.hyh.list

import com.hyh.RefreshActuator
import com.hyh.base.LoadStrategy
import com.hyh.list.internal.IElementDiff
import com.hyh.list.internal.SourceDisplayedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class ItemSource<Param : Any, Item : Any> {

    internal val delegate: Delegate<Param, Item> = object : Delegate<Param, Item>() {

        override fun attach() {
            this@ItemSource.onAttached()
        }

        override fun initPosition(position: Int) {
            _sourcePosition = position
        }

        override fun injectRefreshActuator(refreshActuator: RefreshActuator) {
            _refreshActuator = refreshActuator
        }

        override fun getElementDiff(): IElementDiff<Item> {
            return this@ItemSource.getElementDiff()
        }

        override fun mapItems(items: List<Item>): List<ItemData> {
            return this@ItemSource.mapItems(items)
        }

        override fun updateItemSource(newPosition: Int, newItemSource: ItemSource<Param, Item>) {
            val oldPosition = _sourcePosition
            _sourcePosition = newPosition
            onUpdateItemSource(oldPosition, newPosition, newItemSource)
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
            return this@ItemSource.onResultDisplayed(displayedData)
        }

        override fun detach() {
            _sourcePosition = -1
            this@ItemSource.onDetached()
        }
    }

    private var _sourcePosition: Int = -1
    val sourcePosition: Int
        get() = _sourcePosition

    private lateinit var _refreshActuator: RefreshActuator
    val refreshActuator: RefreshActuator
        get() = _refreshActuator

    protected open fun onAttached() {}
    protected open fun onUpdateItemSource(oldPosition: Int, newPosition: Int, newItemSource: ItemSource<Param, Item>) {}

    protected abstract fun getElementDiff(): IElementDiff<Item>
    protected abstract fun mapItems(items: List<Item>): List<ItemData>
    protected abstract fun onItemsDisplayed(items: List<Item>)
    protected abstract fun onItemsChanged(changes: List<Triple<Item, Item, Any?>>)
    protected abstract fun onItemsRecycled(items: List<Item>)

    open fun getLoadStrategy(): LoadStrategy = LoadStrategy.CancelLast
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

    open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    protected open fun onDetached() {}

    abstract class Delegate<Param : Any, Item : Any> {

        abstract fun attach()

        abstract fun initPosition(position: Int)
        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator)

        abstract fun getElementDiff(): IElementDiff<Item>
        abstract fun mapItems(items: List<Item>): List<ItemData>

        abstract fun updateItemSource(newPosition: Int, newItemSource: ItemSource<Param, Item>)

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

        class Success<Item : Any>() : PreShowResult<Item>() {

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

        class Success<Item : Any>() : LoadResult<Item>() {

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

    data class ItemsBucket(
        val bucketId: Int,
        val itemsToken: Any,
        val items: List<ItemData>,
    )

    class PreShowParams<Param : Any, Item : Any>(
        val param: Param,
        val displayedData: SourceDisplayedData<Item>
    )

    class LoadParams<Param : Any, Item : Any>(
        val param: Param,
        val displayedData: SourceDisplayedData<Item>
    )
}