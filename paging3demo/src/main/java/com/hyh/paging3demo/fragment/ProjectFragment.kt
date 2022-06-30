package com.hyh.paging3demo.fragment


import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hyh.page.IEvent.Companion.unwrapData
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import com.hyh.paging3demo.adapter.ProjectAdapter
import com.hyh.paging3demo.adapter.ProjectLoadStateAdapter
import com.hyh.paging3demo.base.Global
import com.hyh.paging3demo.bean.ProjectChapterBean
import com.hyh.paging3demo.utils.DisplayUtil
import com.hyh.paging3demo.viewmodel.ProjectListViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


class ProjectFragment : CommonBaseFragment() {

    companion object {
        private const val TAG = "ProjectFragment"

        private var _num = 0
    }

    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mRecyclerView: RecyclerView? = null

    private val mProjectAdapter: ProjectAdapter = ProjectAdapter()

    @ExperimentalPagingApi
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
                return context?.let { ProjectListViewModel(it, projectCategory.id, Global.sourceType) } as T
            }
        }
    }


    private var num = _num++

    val job = SupervisorJob() + Dispatchers.Main.immediate

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "Attach：onAttach: $pageContext")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "Attach：onDetach: $pageContext")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.d(TAG, "onDestroyView: $pageContext")
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView: $pageContext")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d(TAG, "onDestroy: $num cancel")

        Log.d(TAG, "onDestroy: $pageContext")
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: $pageContext")


        GlobalScope.launch(job) {
            parentFragment
                ?.pageContext
                ?.eventChannel
                ?.getFlow()
                ?.collect {
                    Log.d(TAG, "collect $num : ${it.unwrapData<Int>()}")
                }
        }

        if (num == 0) {
            job.cancel()
            Log.d(TAG, "onCreate: $num cancel")

        }


        /*lifecycleScope.launch {

        }*/

        parentFragment
            ?.pageContext
            ?.storage
            ?.observe(this, ProjectStore.Num::class.java) {
                Log.d(TAG, "storage: $it")
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



        val concatAdapter: ConcatAdapter = mProjectAdapter.withLoadStateHeaderAndFooter(
            header = ProjectLoadStateAdapter(mProjectAdapter),
            footer = ProjectLoadStateAdapter(mProjectAdapter)
        )
        mRecyclerView?.adapter = concatAdapter

        mSwipeRefreshLayout?.setOnRefreshListener {
            mProjectAdapter.refresh() //执行刷新操作
        }
        lifecycleScope.launchWhenCreated {
            mProjectAdapter.loadStateFlow.collectLatest { loadStates ->
                loadStates.refresh  //获取当前刷新状态
                loadStates.prepend  //获取当前加载上一页状态
                loadStates.append   //获取当前加载下一页状态

                //通常来说，如果我们配合 SwipeRefreshLayout 控件实现下拉刷新，只需要添加以下代码即可
                mSwipeRefreshLayout?.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }
        // FIXME: 2021/5/10  

        /*lifecycleScope.launchWhenCreated {
            mProjectAdapter.loadStateFlow
                // Only emit when REFRESH LoadState for RemoteMediator changes.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where Remote REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                .collect { mRecyclerView?.scrollToPosition(0) }
        }*/
    }

    @ExperimentalPagingApi
    override fun initData() {
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









