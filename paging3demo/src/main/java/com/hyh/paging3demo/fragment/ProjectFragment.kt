package com.hyh.paging3demo.fragment


import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.adapter.ProjectAdapter
import com.hyh.paging3demo.bean.ProjectBean
import com.hyh.paging3demo.bean.ProjectCategoryBean
import com.hyh.paging3demo.utils.DisplayUtil
import com.hyh.paging3demo.viewmodel.ProjectPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ProjectFragment : CommonBaseFragment() {

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mRecyclerView: RecyclerView? = null

    private var mProjectAdapter: ProjectAdapter? = null

    private var mPager: Pager<Int, ProjectBean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPager = context?.let { context ->
            val arguments = arguments!!
            val projectCategory =
                arguments.getParcelable<ProjectCategoryBean>("project_category")!!
            val pager = Pager(PagingConfig(8), initialKey = 1, pagingSourceFactory = {
                ProjectPagingSource(context.applicationContext, projectCategory.id)
            })
            pager
        }
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_project, container, false)
    }

    override fun initView(contentView: View) {
        mSwipeRefreshLayout = contentView.findViewById(R.id.swipe_refresh_layout)
        mRecyclerView = contentView.findViewById(R.id.recycler_view)
        mRecyclerView?.apply {
            layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = DisplayUtil.dip2px(view.getContext(), 8F)
                }
            })
        }
        mSwipeRefreshLayout?.setOnRefreshListener {
            mProjectAdapter?.refresh()
        }
        mProjectAdapter = ProjectAdapter(object : DiffUtil.ItemCallback<ProjectBean>() {

            override fun areItemsTheSame(oldItem: ProjectBean, newItem: ProjectBean): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProjectBean, newItem: ProjectBean): Boolean {
                return oldItem.id == newItem.id
            }
        })
        mRecyclerView?.adapter = mProjectAdapter
    }

    override fun initData() {
        /*mProjectAdapter?.addLoadStateListener {
            mProjectAdapter?.refresh()
        }*/


        lifecycleScope.launch(Dispatchers.IO) {
            mPager?.flow
                ?.flowOn(Dispatchers.Main)
                ?.collect {
                mProjectAdapter?.submitData(it)
            }
        }
    }
}