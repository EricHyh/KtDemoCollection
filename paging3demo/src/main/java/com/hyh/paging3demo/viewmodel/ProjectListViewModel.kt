package com.hyh.paging3demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.db.ProjectDB
import kotlinx.coroutines.flow.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/1/13
 */
@ExperimentalPagingApi
class ProjectListViewModel(
    private val context: Context,
    private val chapterId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "ProjectListViewModel"
    }

    private val mPager: Pager<Int, ProjectBean> =
        Pager(
            config = PagingConfig(30, enablePlaceholders = false),
            remoteMediator = ProjectRemoteMediator(
                context,
                ProjectDB.get(context),
                chapterId = chapterId
            )
        ) {
            ProjectDB.get(context).projects().getProjectsByChapterId(chapterId)
            //ProjectPagingSource(context,chapterId)
            /*object : PagingSource<Int, ProjectBean>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProjectBean> {
                    return LoadResult.Page(emptyList(), null, null)
                }
            }*/
        }

    val projects = mPager.flow.cachedIn(viewModelScope)

}