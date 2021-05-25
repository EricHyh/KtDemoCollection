package com.hyh.paging3demo.viewmodel

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.cachedIn
import com.hyh.paging3demo.bean.ProjectChapterBean
import com.hyh.paging3demo.bean.ProjectChaptersBean
import com.hyh.paging3demo.fragment.ProjectFragment
import com.hyh.paging3demo.net.RetrofitHelper
import com.hyh.tabs.FragmentTab
import com.hyh.tabs.TabInfo
import com.hyh.tabs.TabSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.http.GET

class ProjectChaptersViewModel2(context: Context) : ViewModel() {


    private val projectTabSource = ProjectTabSource(context)

    val flow = projectTabSource.flow

    class ProjectTabSource(private val context: Context) : TabSource<Unit, FragmentTab>(Unit) {

        private val TAG = "ProjectTabSource"

        private val mProjectChaptersApi: ProjectChaptersApi

        init {
            mProjectChaptersApi = RetrofitHelper.create(context, ProjectChaptersApi::class.java)
        }

        var num = 0

        override suspend fun load(param: Unit): LoadResult<FragmentTab> {
            val result = kotlin.runCatching {
                mProjectChaptersApi.get().execute()
            }
            delay(3000)
            if (result.isSuccess) {
                val response = result.getOrNull() ?: return LoadResult.Error(NullPointerException())
                if (response.isSuccessful) {
                    val projectChaptersBean: ProjectChaptersBean = response.body() ?: return LoadResult.Success(emptyList())
                    val projectChapters = projectChaptersBean.projectChapters ?: return LoadResult.Success(emptyList())
                    var tabs = projectChapters.map { bean ->
                        TabInfo(
                            lazyTab = lazy {
                                val bundle = Bundle()
                                bundle.putParcelable("project_chapter", bean)
                                FragmentTab(Fragment.instantiate(context, ProjectFragment::class.java.name, bundle))
                            },
                            tabToken = bean,
                            tabTitle = bean.name
                        )
                    }
                    if (++num % 2 == 0) {
                        tabs = tabs.reversed()
                    }
                    return LoadResult.Success(tabs)
                } else {
                    return LoadResult.Error(HttpException(response))
                }
            } else {
                return LoadResult.Error(result.exceptionOrNull() ?: NullPointerException())
            }
        }

        override fun getFetchDispatcher(param: Unit): CoroutineDispatcher {
            return Dispatchers.IO
        }


        internal interface ProjectChaptersApi {

            @GET("/project/tree/json")
            fun get(): Call<ProjectChaptersBean>
        }
    }
}