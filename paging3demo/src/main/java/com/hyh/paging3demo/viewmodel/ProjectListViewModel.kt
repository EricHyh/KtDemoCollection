package com.hyh.paging3demo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.bean.ProjectCategoryBean

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/1/13
 */
class ProjectListViewModel(private val context: Context, private val categoryId: Int) : ViewModel() {
    companion object {
        private const val TAG = "ProjectListViewModel"
    }

    private val mPager: Pager<Int, ProjectBean> = Pager(PagingConfig(8), initialKey = 1, pagingSourceFactory = {
        ProjectPagingSource(context.applicationContext, categoryId)
    })


}

class ProjectListViewModelFactory(private val context: Context, private val categoryId: Int) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProjectListViewModel(context, categoryId) as T
    }
}