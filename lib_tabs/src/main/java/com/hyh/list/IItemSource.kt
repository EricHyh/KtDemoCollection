package com.hyh.list

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface IItemSource<Param : Any> {

    suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult
    suspend fun onPreShowResult(params: PreShowParams<Param>, preShowResult: PreShowResult) {}
    suspend fun load(params: LoadParams<Param>): LoadResult
    suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {}
    fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class PreShowResult {

        object Unused : PreShowResult()

        data class Success constructor(
            val items: List<ItemData>
        ) : PreShowResult()
    }

    sealed class LoadResult {

        data class Error(
            val error: Throwable
        ) : LoadResult()

        data class Success constructor(
            val items: List<ItemData>
        ) : LoadResult()
    }

    class PreShowParams<Param : Any>(
        val param: Param,
        val lastPreShowResult: PreShowResult?,
        val lastLoadResult: LoadResult?
    )

    class LoadParams<Param : Any>(
        val param: Param,
        val lastPreShowResult: PreShowResult?,
        val lastLoadResult: LoadResult?
    )
}