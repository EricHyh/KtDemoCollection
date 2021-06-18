package com.hyh.list

import com.hyh.list.internal.ItemSourceFetcher
import com.hyh.list.internal.RepoData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class ItemSourceRepository<Param : Any>(initialParam: Param?) {

    private val itemSourceFetcher = object : ItemSourceFetcher<Param>(initialParam) {

        override suspend fun getCache(params: CacheParams<Param>): CacheResult =
            this@ItemSourceRepository.getCache(params)

        override suspend fun onCacheResult(params: CacheParams<Param>, cacheResult: CacheResult) =
            this@ItemSourceRepository.onCacheResult(params, cacheResult)

        override suspend fun load(params: LoadParams<Param>): LoadResult =
            this@ItemSourceRepository.load(params)

        override suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) =
            this@ItemSourceRepository.onLoadResult(params, loadResult)

        override fun getFetchDispatcher(param: Param): CoroutineDispatcher =
            this@ItemSourceRepository.getFetchDispatcher(param)

    }

    val flow: Flow<RepoData<Param>> = itemSourceFetcher.flow

    protected abstract suspend fun getCache(params: CacheParams<Param>): CacheResult

    protected open suspend fun onCacheResult(params: CacheParams<Param>, cacheResult: CacheResult) {}

    protected abstract suspend fun load(params: LoadParams<Param>): LoadResult

    protected open suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {}

    protected open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class CacheResult {

        object Unused : CacheResult()

        class Success(
            val sources: List<ItemSourceInfo>,
        ) : CacheResult()
    }

    sealed class LoadResult {

        class Error(val error: Throwable) : LoadResult()

        class Success(
            val sources: List<ItemSourceInfo>,
        ) : LoadResult()
    }

    data class ItemSourceInfo(
        val sourceToken: Any,
        val paramProvider: IParamProvider<out Any>,
        val source: ItemSource<out Any>,
    )

    class CacheParams<Param : Any>(
        val param: Param,
        val lastCacheResult: CacheResult?,
        val lastLoadResult: LoadResult?
    )

    class LoadParams<Param : Any>(
        val param: Param,
        val lastCacheResult: CacheResult?,
        val lastLoadResult: LoadResult?
    )
}