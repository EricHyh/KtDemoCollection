package com.hyh.paging3demo.fragment


import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.adapter.ProjectAdapter
import com.hyh.paging3demo.bean.ProjectChapterBean
import com.hyh.paging3demo.utils.DisplayUtil
import com.hyh.paging3demo.viewmodel.ProjectListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalPagingApi
class ProjectFragment : CommonBaseFragment() {

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mRecyclerView: RecyclerView? = null

    private val mProjectAdapter: ProjectAdapter = ProjectAdapter()

    private val mProjectListViewModel: ProjectListViewModel? by viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T {
                val arguments = arguments!!
                val projectCategory =
                    arguments.getParcelable<ProjectChapterBean>("project_chapter")!!
                @Suppress("UNCHECKED_CAST")
                return context?.let { ProjectListViewModel(it, projectCategory.id) } as T
            }
        }
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_project, container, false)
    }

    override fun initView(contentView: View) {
        mSwipeRefreshLayout = contentView.findViewById(R.id.swipe_refresh_layout)
        mRecyclerView = contentView.findViewById(R.id.recycler_view)
        mRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = DisplayUtil.dip2px(view.context, 8F)
                }
            })
        }
        mSwipeRefreshLayout?.setOnRefreshListener {
            mProjectAdapter.refresh()
        }
        mRecyclerView?.adapter = mProjectAdapter

        /*lifecycleScope.launchWhenCreated {
            mProjectAdapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { mRecyclerView?.scrollToPosition(0) }
        }*/
    }

    override fun initData() {
        lifecycleScope.launchWhenCreated {
            mProjectAdapter.loadStateFlow.collectLatest { loadStates ->
                mSwipeRefreshLayout?.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        /*mProjectListViewModel?.projects?.asLiveData()?.observe(this) {
            mProjectAdapter.submitData(lifecycle, it)
        }*/

        lifecycleScope.launchWhenCreated {
            mProjectListViewModel?.projects?.collectLatest {
                mProjectAdapter.submitData(it)
            }
        }
    }
}