package com.hyh.paging3demo.viewmodel

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import com.hyh.paging3demo.api.ProjectApi
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.net.RetrofitHelper

class ProjectPagingSource(context: Context, private val mCid: Int) :
    PagingSource<Int, ProjectBean>() {

    private val mProjectApi: ProjectApi = RetrofitHelper.create(context, ProjectApi::class.java)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProjectBean> {
        val pageIndex = params.key ?: 1
        Log.d("ProjectPagingSource", "load -> ${params} - $pageIndex")
        return try {
            val projectsBean = mProjectApi.get(pageIndex, mCid)
            if (projectsBean.data.projects?.isEmpty() != false) {
                if (projectsBean.data.curPage == projectsBean.data.pageCount) {
                    LoadResult.Page(emptyList(), if (pageIndex == 1) null else pageIndex - 1, null)
                } else {
                    LoadResult.Error(NullPointerException())
                }
            } else {
                LoadResult.Page(projectsBean.data.projects, if (pageIndex == 1) null else pageIndex - 1, pageIndex + 1)
            }
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }
}