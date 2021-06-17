package com.hyh.paging3demo.list

import com.hyh.list.EmptyParamProvider
import com.hyh.list.SimpleItemSourceRepository

class NumItemSourceRepo : SimpleItemSourceRepository<Unit>(Unit) {

    override suspend fun getCacheWhenTheFirstTime(param: Unit): CacheResult {
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