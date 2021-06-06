package com.hyh.list

interface IItemSource<Param : Any> {

    fun getSourceToken(): Any

    suspend fun gePreShow(param: Param): CacheResult

    suspend fun load(param: Param): LoadResult

    sealed class CacheResult {

        object Unused : CacheResult()

        data class Success constructor(
            val tabs: List<ItemData>
        ) : CacheResult()
    }

    sealed class LoadResult {

        data class Error(
            val error: Throwable
        ) : LoadResult()

        data class Success constructor(
            val tabs: List<ItemData>
        ) : LoadResult()
    }

}