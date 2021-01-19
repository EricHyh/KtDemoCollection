package com.hyh.paging3demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.db.ProjectDB

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/1/13
 */
/*@ExperimentalPagingApi
class ProjectListViewModel(private val context: Context, private val chapterId: Int) : ViewModel() {

    private val mPager: Pager<Int, ProjectBean> = Pager(
        config = PagingConfig(10, prefetchDistance = 1, enablePlaceholders = false),
        pagingSourceFactory = {
            ProjectPagingSource(context, chapterId)
        }
    )

    val projects = mPager.flow.cachedIn(viewModelScope)

}*/


@ExperimentalPagingApi
class ProjectListViewModel(private val context: Context, private val chapterId: Int) : ViewModel() {

    private val mPager: Pager<Int, ProjectBean> =
        Pager(
            config = PagingConfig(10, prefetchDistance = 1, enablePlaceholders = false),
            remoteMediator = ProjectRemoteMediator(context, ProjectDB.get(context), chapterId = chapterId),
            pagingSourceFactory = {
                ProjectDB.get(context).projects().getProjectsByChapterId(chapterId)
            }
        )

    val projects = mPager.flow.cachedIn(viewModelScope)

}
