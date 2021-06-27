package com.hyh.list

class SingleItemSourceRepository(private val itemSource: ItemSource<*>) : SimpleItemSourceRepository<Unit>(Unit) {

    override suspend fun getCacheWhenTheFirstTime(param: Unit): CacheResult {
        return CacheResult.Success(
            listOf(
                ItemSourceInfo(Unit, itemSource)
            )
        )
    }

    override suspend fun load(param: Unit): LoadResult {
        return LoadResult.Success(
            listOf(
                ItemSourceInfo(Unit, itemSource)
            )
        )
    }
}