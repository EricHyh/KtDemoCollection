package com.hyh.list

abstract class ItemSourceRepository {

    abstract suspend fun getCache(): CacheResult

    abstract suspend fun load(): LoadResult

    sealed class CacheResult {

        object Unused : CacheResult()

        class Success(
            val sources: List<ItemSourceInfo>,
        ) : CacheResult()
    }

    sealed class LoadResult {

        class Error(error: Throwable) : LoadResult()

        class Success(
            val sources: List<ItemSourceInfo>,
        ) : LoadResult()
    }
}

data class ItemSourceInfo(
    val sourceToken: Any,
    val lazySource: Lazy<IItemSource<out Any>>
)