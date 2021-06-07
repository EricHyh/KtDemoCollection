package com.hyh.list

abstract class ItemSourceRepository {

    abstract suspend fun getCache(): CacheResult

    abstract suspend fun load(): LoadResult

    sealed class CacheResult {

        object Unused : CacheResult()

        class Success(
            val sources: List<IItemSource<out Any>>,
        ) : CacheResult()
    }

    sealed class LoadResult {

        class Error(error: Throwable) : LoadResult()

        class Success(
            val sources: List<IItemSource<out Any>>,
        ) : LoadResult()
    }
}