package com.hyh.paging3demo.list

import com.hyh.list.EmptyParamProvider
import com.hyh.list.ItemSourceInfo
import com.hyh.list.ItemSourceRepository

class NumItemSourceRepo : ItemSourceRepository<Unit>(Unit) {

    override suspend fun getCache(param: Unit, completeTimes: Int): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        val sources = ListConfig.randomTypes()
            .map {
                ItemSourceInfo(
                    it,
                    EmptyParamProvider,
                    lazy {
                        NumItemSource(it)
                    }
                )
            }
        return LoadResult.Success(sources)
    }
}