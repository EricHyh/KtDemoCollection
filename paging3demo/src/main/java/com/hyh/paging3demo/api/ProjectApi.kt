package com.hyh.paging3demo.api

import com.hyh.paging3demo.bean.ProjectsBean
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface ProjectApi {

    //https://www.wanandroid.com/project/list/1/json?cid=294
    @GET("/project/list/{pageIndex}/json")
    suspend fun get(
        @Path("pageIndex") pageIndex: Int,
        @Query("cid") cid: Int
    ): ProjectsBean



}