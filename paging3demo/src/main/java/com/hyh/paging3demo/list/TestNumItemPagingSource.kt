package com.hyh.paging3demo.list

import com.hyh.list.FlatListItem
import com.hyh.list.SimpleItemPagingSource

/**
 * TODO: Add Description
 *
 * @author eriche 2022/6/21
 */
class TestNumItemPagingSource : SimpleItemPagingSource<Int>(0) {
    companion object {

        private const val TAG = "TestNumItemPagingSource"
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FlatListItem> {
        when (params) {
            is LoadParams.Refresh -> {
                val items = mutableListOf<FlatListItem>()
                for (index in 0..20) {
                    items.add(NumFlatListItem(params.param?.toString() ?: "", index, index))
                }
                return LoadResult.Success(items, 1)
            }
            is LoadParams.Append -> {
                val items = mutableListOf<FlatListItem>()
                val param = params.param ?: 1
                for (index in 0..20) {
                    items.add(NumFlatListItem(param.toString(), index, index))
                }
                return LoadResult.Success(items, param + 1, param == 20)
            }
        }
    }

    override suspend fun getRefreshKey(): Int? {
        return null
    }

    override val sourceToken: Any
        get() = TestNumItemPagingSource::class.java
}