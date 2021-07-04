package com.hyh.list

class SingleItemSourceRepo(private val itemSource: ItemSource<*, *>) : SimpleItemSourceRepo<Unit>(Unit) {

    override suspend fun getCache(param: Unit): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        return LoadResult.Success(listOf(itemSource))
    }
}