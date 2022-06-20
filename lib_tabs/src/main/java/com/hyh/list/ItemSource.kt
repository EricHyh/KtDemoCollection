package com.hyh.list

import androidx.lifecycle.LifecycleOwner
import com.hyh.base.RefreshStrategy
import com.hyh.list.internal.BaseItemSource
import com.hyh.list.internal.SourceDisplayedData

/**
 * [IFlatListItem]列表的数据源
 *
 * @param Param 参数泛型
 * @param Item 原始的 Item 数据泛型
 */
abstract class ItemSource<Param : Any, Item : Any> : BaseItemSource<Param, Item>(), LifecycleOwner {


    protected open fun areSourceTheSame(newItemSource: ItemSource<Param, Item>): Boolean {
        return this.javaClass == newItemSource.javaClass
    }

    protected open fun onUpdateItemSource(newItemSource: ItemSource<Param, Item>) {}


    open fun getRefreshStrategy(): RefreshStrategy = RefreshStrategy.CancelLast
    abstract suspend fun getParam(): Param
    abstract suspend fun getPreShow(params: PreShowParams<Param, Item>): PreShowResult<Item>
    abstract suspend fun load(params: LoadParams<Param, Item>): LoadResult<Item>

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