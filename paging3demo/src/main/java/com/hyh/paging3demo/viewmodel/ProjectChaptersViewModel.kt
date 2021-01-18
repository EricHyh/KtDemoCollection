package com.hyh.paging3demo.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hyh.paging3demo.bean.ProjectChaptersBean
import com.hyh.paging3demo.net.RetrofitHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET

class ProjectChaptersViewModel(context: Context) : ViewModel() {

    val mutableLiveData: MutableLiveData<ProjectChaptersBean> = MutableLiveData()
    private val mProjectChaptersApi: ProjectChaptersApi

    init {
        mProjectChaptersApi = RetrofitHelper.create(context, ProjectChaptersApi::class.java)
    }


    fun loadData() {
        mProjectChaptersApi.get().enqueue(object : Callback<ProjectChaptersBean> {
            override fun onResponse(
                call: Call<ProjectChaptersBean>,
                response: Response<ProjectChaptersBean>
            ) {
                if (response.isSuccessful) {
                    mutableLiveData.setValue(response.body())
                } else {
                    mutableLiveData.setValue(null)
                }
            }

            override fun onFailure(call: Call<ProjectChaptersBean>, t: Throwable) {
                mutableLiveData.value = null
            }
        })
    }

    internal interface ProjectChaptersApi {

        @GET("/project/tree/json")
        fun get(): Call<ProjectChaptersBean>
    }
}