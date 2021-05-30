package com.hyh.list

interface IItemSource<Param : Any, Data : IItemData> {

    fun getSourceToken(): Any

    suspend fun gePreShow(param: Param): CacheResult<Data>

    suspend fun load(param: Param): LoadResult<Data>

    sealed class CacheResult<Data : IItemData> {

        class Unused<Data : IItemData> : CacheResult<Data>()

        data class Success<Data : IItemData> constructor(
            val tabs: List<Data>
        ) : CacheResult<Data>()
    }

    sealed class LoadResult<Data : IItemData> {

        data class Error<Data : IItemData>(
            val error: Throwable
        ) : LoadResult<Data>()

        data class Success<Data : IItemData> constructor(
            val tabs: List<Data>
        ) : LoadResult<Data>()
    }
}