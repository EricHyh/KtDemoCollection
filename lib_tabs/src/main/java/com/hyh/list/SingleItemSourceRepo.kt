package com.hyh.list

import com.hyh.ObjectProvider

class SingleItemSourceRepo(private val itemSourceProvider: ObjectProvider<ItemSource<*, *>>) :
    SimpleItemSourceRepo<Unit>(Unit) {

    constructor(itemSource: ItemSource<*, *>) : this({ itemSource })

    override suspend fun getCache(param: Unit): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        return LoadResult.Success(listOf(itemSourceProvider()))
    }
}