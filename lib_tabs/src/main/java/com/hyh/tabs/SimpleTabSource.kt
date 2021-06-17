package com.hyh.tabs


/**
 * 实现了只在第一次获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleTabSource<Param : Any, Tab : ITab> : TabSource<Param, Tab>() {

    final override suspend fun getCache(params: CacheParams<Param, Tab>): CacheResult<Tab> {
        return if (params.lastLoadResult == null && params.lastCacheResult == null) {
            getCacheWhenTheFirstTime(params.param)
        } else {
            CacheResult.Unused()
        }
    }

    final override suspend fun onCacheResult(params: CacheParams<Param, Tab>, result: CacheResult<Tab>) {
        super.onCacheResult(params, result)
    }

    final override suspend fun load(params: LoadParams<Param, Tab>): LoadResult<Tab> {
        return load(params.param)
    }

    final override suspend fun onLoadResult(params: LoadParams<Param, Tab>, result: LoadResult<Tab>) {
        super.onLoadResult(params, result)
    }

    protected abstract suspend fun getCacheWhenTheFirstTime(param: Param): CacheResult<Tab>

    protected abstract suspend fun load(param: Param): LoadResult<Tab>

}