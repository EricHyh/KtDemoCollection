package com.hyh.list

/**
 * 实现了只在第一次获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleItemSourceRepo<Param : Any>(initialParam: Param) : ItemSourceRepo<Param>(initialParam) {

    final override suspend fun getCache(params: CacheParams<Param>): CacheResult {
        if (params.displayedData.lazySources == null) {
            return getCache(params.param)
        }
        return CacheResult.Unused
    }

    final override suspend fun load(params: LoadParams<Param>): LoadResult {
        return load(params.param)
    }

    protected abstract suspend fun getCache(param: Param): CacheResult

    protected abstract suspend fun load(param: Param): LoadResult

}