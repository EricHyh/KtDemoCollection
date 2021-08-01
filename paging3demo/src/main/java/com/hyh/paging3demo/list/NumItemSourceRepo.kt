package com.hyh.paging3demo.list

import com.hyh.list.SimpleItemSourceRepo
import com.hyh.base.RefreshStrategy
import com.hyh.list.internal.RepoDisplayedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class NumItemSourceRepo : SimpleItemSourceRepo<Unit>(Unit) {


    override fun getRefreshStrategy(): RefreshStrategy {
        return RefreshStrategy.DelayedQueueUp(5000)
        //return RefreshStrategy.QueueUp
        //return RefreshStrategy.CancelLast
    }

    override suspend fun getCache(param: Unit): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        delay(1000)
        //SystemClock.sleep(1000)
        val sources = listOf(NumItemSource("A"))


        /*val sources = ListConfig.randomTypes()
            .map {
                ItemSourceInfo(
                    it,
                    NumItemSource(it)
                )
            }*/
        return LoadResult.Success(sources)
    }

    override fun getFetchDispatcher(param: Unit, displayedData: RepoDisplayedData): CoroutineDispatcher {
        return Dispatchers.IO
    }

}