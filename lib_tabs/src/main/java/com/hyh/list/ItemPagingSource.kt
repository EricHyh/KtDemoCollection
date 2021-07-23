package com.hyh.list

import androidx.paging.PagingState

abstract class ItemPagingSource<Param : Any, Item : Any>(initialParam: Any?) {

    abstract val sourceToken: Any

    abstract suspend fun load(params: LoadParams<Param>): LoadResult<Param, Item>

    abstract fun getRefreshKey(): Param?

    abstract class Delegate {

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