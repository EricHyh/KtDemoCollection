package com.hyh.paging3demo.list

import android.os.SystemClock
import com.hyh.list.SimpleItemSourceRepository
import com.hyh.list.internal.RefreshStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class MultiTabsItemSourceRepo : SimpleItemSourceRepository<Unit>(Unit) {


    override fun getRefreshStrategy(): RefreshStrategy {
        return RefreshStrategy.DelayedQueueUp(5000)
        //return RefreshStrategy.QueueUp
        //return RefreshStrategy.CancelLast
    }

    override suspend fun getCacheWhenTheFirstTime(param: Unit): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        delay(1000)
        //SystemClock.sleep(1000)

        val sources = ListConfig.randomTypes()
            .map {
                ItemSourceInfo(
                    it,
                    TestMultiTabsItemSource()
                )
            }
        return LoadResult.Success(sources)
    }

    override fun getFetchDispatcher(param: Unit): CoroutineDispatcher {
        return Dispatchers.IO
    }

}