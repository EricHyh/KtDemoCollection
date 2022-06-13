package com.hyh.list

import com.hyh.RefreshActuator
import com.hyh.lifecycle.ChildLifecycleOwner
import com.hyh.lifecycle.IChildLifecycleOwner
import com.hyh.list.internal.IElementDiff
import com.hyh.list.internal.SourceDisplayedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class ItemPagingSource<Param : Any, Item : Any>(val initialParam: Any?) {


    internal val delegate: Delegate<Param, Item> = object : Delegate<Param, Item>() {
        override val lifecycleOwner: ChildLifecycleOwner
            get() = TODO("Not yet implemented")

        override fun attach() {
            TODO("Not yet implemented")
        }

        override fun injectRefreshActuator(refreshActuator: RefreshActuator) {
            TODO("Not yet implemented")
        }

        override fun getElementDiff(): IElementDiff<Item> {
            TODO("Not yet implemented")
        }

        override fun mapItems(items: List<Item>): List<FlatListItem> {
            TODO("Not yet implemented")
        }

        override fun areSourceTheSame(newItemSource: ItemSource<Param, Item>): Boolean {
            TODO("Not yet implemented")
        }

        override fun updateItemSource(newItemSource: ItemSource<Param, Item>) {
            TODO("Not yet implemented")
        }

        override fun onItemsDisplayed(items: List<Item>) {
            TODO("Not yet implemented")
        }

        override fun onItemsChanged(changes: List<Triple<Item, Item, Any?>>) {
            TODO("Not yet implemented")
        }

        override fun onItemsRecycled(items: List<Item>) {
            TODO("Not yet implemented")
        }

        override fun onProcessResult(
            resultItems: List<Item>,
            resultExtra: Any?,
            displayedData: SourceDisplayedData<Item>
        ) {
            TODO("Not yet implemented")
        }

        override fun onResultDisplayed(displayedData: SourceDisplayedData<Item>) {
            TODO("Not yet implemented")
        }

        override fun detach() {
            TODO("Not yet implemented")
        }
    }

    abstract val sourceToken: Any

    abstract suspend fun load(params: LoadParams<Param>): LoadResult<Param, Item>

    abstract fun getRefreshKey(): Param?

    protected open fun onAttached() {}

    protected open fun onDetached() {}


    protected abstract fun getElementDiff(): IElementDiff<Item>
    protected abstract fun mapItems(items: List<Item>): List<FlatListItem>

    open fun getFetchDispatcher(
        param: Param,
        displayedData: SourceDisplayedData<Item>
    ): CoroutineDispatcher =
        Dispatchers.Unconfined

    open fun getProcessDataDispatcher(
        param: Param,
        displayedData: SourceDisplayedData<Item>
    ): CoroutineDispatcher =
        getFetchDispatcher(param, displayedData)

    abstract class Delegate<Param : Any, Item : Any> : IChildLifecycleOwner {

        abstract fun attach()

        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator)

        abstract fun getElementDiff(): IElementDiff<Item>
        abstract fun mapItems(items: List<Item>): List<FlatListItem>

        abstract fun areSourceTheSame(newItemSource: ItemSource<Param, Item>): Boolean
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

    sealed class LoadParams<Param : Any> {

        abstract val param: Param?

        class Refresh<Param : Any>(override val param: Param?) : LoadParams<Param>()

        class Append<Param : Any>(override val param: Param?) : LoadParams<Param>()
    }

    sealed class LoadResult<Param : Any, Item : Any> {

        data class Error<Param : Any, Item : Any>(
            val throwable: Throwable
        ) : LoadResult<Param, Item>()

        data class Success<Param : Any, Item : Any>(
            val items: List<Item>,
            val nextParam: Param?
        ) : LoadResult<Param, Item>()
    }
}