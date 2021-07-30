package com.hyh.list

class MultiItemSourceRepo(private val itemSources: List<ItemSource<out Any, out Any>>) : SimpleItemSourceRepo<Unit>(Unit) {

    override suspend fun getCache(param: Unit): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        return LoadResult.Success(itemSources)
    }
}