package com.hyh.list

/**
 * 实现了只在第一次获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleItemSourceRepository<Param : Any>(initialParam: Param?) : ItemSourceRepository<Param>(initialParam) {

    final override suspend fun getCache(params: CacheParams<Param>): CacheResult {
        return if (params.lastLoadResult == null && params.lastCacheResult == null) {
            getCacheWhenTheFirstTime(params.param)
        } else {
            CacheResult.Unused
        }
    }

    final override suspend fun onCacheResult(params: CacheParams<Param>, cacheResult: CacheResult) {
        super.onCacheResult(params, cacheResult)
    }

    final override suspend fun load(params: LoadParams<Param>): LoadResult {
        return load(params.param)
    }

    final override suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {
        super.onLoadResult(params, loadResult)
    }

    protected abstract suspend fun getCacheWhenTheFirstTime(param: Param): CacheResult

    protected abstract suspend fun load(param: Param): LoadResult

}