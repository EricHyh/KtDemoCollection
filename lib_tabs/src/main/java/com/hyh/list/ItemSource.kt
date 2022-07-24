package com.hyh.list

import androidx.lifecycle.LifecycleOwner
import com.hyh.base.RefreshStrategy
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.SourceDisplayedData

/**
 * [IFlatListItem]列表的数据源
 *
 * @param Param 参数泛型
 * @param Item 原始的 Item 数据泛型
 */
abstract class ItemSource<Param, Item> : BaseItemSource<Param, Item>(), LifecycleOwner {


    protected open fun areSourceTheSame(newItemSource: ItemSource<Param, Item>): Boolean {
        return this.javaClass == newItemSource.javaClass
    }

    protected open fun onUpdateItemSource(newItemSource: ItemSource<Param, Item>) {}


    open fun getRefreshStrategy(): RefreshStrategy = RefreshStrategy.CancelLast
    abstract suspend fun getParam(): Param
    abstract suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult<Item>
    abstract suspend fun load(params: LoadParams<Param>): LoadResult<Item>

    sealed class PreShowResult<Item> {

        class Unused<Item> : PreShowResult<Item>()

        class Success<Item> private constructor() : PreShowResult<Item>() {

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

    sealed class LoadResult<Item> {

        class Error<Item>(
            val error: Throwable
        ) : LoadResult<Item>()

        class Success<Item> private constructor() : LoadResult<Item>() {

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

    class PreShowParams<Param>(
        val param: Param,
        val displayedData: SourceDisplayedData
    )

    class LoadParams<Param>(
        val param: Param,
        val displayedData: SourceDisplayedData
    )
}