package com.hyh.list

import com.hyh.list.internal.BaseItemSource

abstract class ItemPagingSource<Param : Any, Item : Any>(val initialParam: Param?) : BaseItemSource<Param, Item>() {

    abstract suspend fun load(params: LoadParams<Param>): LoadResult<Param, Item>

    abstract fun getRefreshKey(): Param?


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