package com.hyh.paging3demo.list

import android.util.Log
import com.hyh.list.SimpleItemSourceRepo
import com.hyh.base.RefreshStrategy
import com.hyh.list.SingleItemSource
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

    var num = 0

    override suspend fun load(param: Unit): LoadResult {
        //delay(1000)
        //SystemClock.sleep(1000)
        val sources =
            listOf(
                //NumItemSource("${num}"),
                SingleItemSource(TextSourceStateItem(TestNumItemPagingSource::class.java)),
                TestNumItemPagingSource(),
                SingleItemSource(TextAppendStateItem(TestNumItemPagingSource::class.java)),
            )

        /*val sources = ListConfig.randomTypes()
            .map {
                ItemSourceInfo(
                    it,
                    NumItemSource(it)
                )
            }*/
        Log.d("TAG", "load: ")
        return LoadResult.Success(sources)
    }

    override fun getFetchDispatcher(param: Unit, displayedData: RepoDisplayedData): CoroutineDispatcher {
        return Dispatchers.IO
    }

}



