package com.hyh.paging3demo.viewmodel

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.bean.ProjectsBean
import com.hyh.paging3demo.net.RetrofitHelper
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class ProjectPagingSource(context: Context, private val mCid: Int) :
    PagingSource<Int, ProjectBean>() {

    private val mProjectApi: ProjectApi

    init {
        this.mProjectApi = RetrofitHelper.create(context, ProjectApi::class.java)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProjectBean> {
        val pageIndex = params.key ?: 1
        Log.d( "ProjectPagingSource", "load -> $pageIndex")
        return try {
            val response = mProjectApi[pageIndex, mCid].execute()
            if (response.isSuccessful) {
                val projectsBean = response.body()
                LoadResult.Page(
                    projectsBean?.data?.projects ?: emptyList(),
                    if (pageIndex == 1) null else pageIndex - 1,
                    pageIndex + 1
                )
            } else {
                LoadResult.Error(HttpException(response))
            }
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }

    internal interface ProjectApi {

        //https://www.wanandroid.com/project/list/1/json?cid=294
        @GET("/project/list/{pageIndex}/json")
        operator fun get(@Path("pageIndex") pageIndex: Int, @Query("cid") cid: Int): Call<ProjectsBean>

    }
}