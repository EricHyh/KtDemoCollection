package com.hyh.paging3demo.viewmodel

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hyh.paging3demo.api.ProjectApi
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.net.RetrofitHelper

class ProjectPagingSource(context: Context, private val chapterId: Int) :
    PagingSource<Int, ProjectBean>() {

    private val mProjectApi: ProjectApi = RetrofitHelper.create(context, ProjectApi::class.java)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProjectBean> {
        val pageIndex = params.key ?: 1
        try {
            val projectsBean = mProjectApi.get(pageIndex, chapterId)
            if (projectsBean.data == null) {
                return LoadResult.Error(NullPointerException())
            }
            val prevKey = if (pageIndex <= 1) null else pageIndex - 1
            val nextKey = if (projectsBean.data.curPage >= projectsBean.data.pageCount) null else pageIndex + 1
            return LoadResult.Page(
                projectsBean.data.projects ?: emptyList(),
                prevKey,
                nextKey
            )
        } catch (e: Throwable) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ProjectBean>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}