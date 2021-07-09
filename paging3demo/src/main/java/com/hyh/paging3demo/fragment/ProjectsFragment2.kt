package com.hyh.paging3demo.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hyh.page.IEvent.Companion.asEvent
import com.hyh.page.IStore
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import com.hyh.paging3demo.viewmodel.ContextViewModelFactory
import com.hyh.paging3demo.viewmodel.ProjectChaptersViewModel2
import com.hyh.tabs.adapter.FragmentTabAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProjectsFragment2 : CommonBaseFragment() {

    private val TAG = "ProjectsFragment2"

    private var mProjectChaptersViewModel: ProjectChaptersViewModel2? = null

    private var mFragmentTabAdapter: FragmentTabAdapter<Unit>? = null

    private var mTabLayout: TabLayout? = null

    private var mViewPager: ViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProjectChaptersViewModel = ViewModelProvider(
            this,
            ContextViewModelFactory(context!!)
        ).get(ProjectChaptersViewModel2::class.java)

        mFragmentTabAdapter = FragmentTabAdapter<Unit>(pageContext, childFragmentManager)


        pageContext.storage.store(ProjectStore.Num(100))

        Log.d(TAG, "onCreate: $pageContext")
        Log.d(TAG, "onCreate: $viewModelStore")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: $pageContext")
        Log.d(TAG, "onDestroy: $viewModelStore")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach: $pageContext")
        Log.d(TAG, "onDetach: $viewModelStore")
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_projects2, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    var num: Int = 0

    override fun initView(contentView: View) {
        contentView.findViewById<Button>(R.id.btn_refresh1)
            .setOnClickListener {
                mFragmentTabAdapter?.refresh(Unit)
                mFragmentTabAdapter?.refresh(Unit)
                mFragmentTabAdapter?.refresh(Unit)

                pageContext
                    .eventChannel
                    .send((num++).asEvent())

                pageContext.storage.store(ProjectStore.Num(num))
            }


        contentView.findViewById<Button>(R.id.btn_refresh2)
            .setOnClickListener {
                pageContext
                    .eventChannel
                    .send((num++).asEvent())
                pageContext.storage.store(ProjectStore.Num(num))
            }


        mViewPager = contentView.findViewById<ViewPager>(R.id.view_pager)
        mViewPager!!.adapter = mFragmentTabAdapter
        mViewPager!!.offscreenPageLimit = 1




        mTabLayout = contentView.findViewById(R.id.tab_layout)
        mTabLayout?.setupWithViewPager(mViewPager)



        lifecycleScope.launch {
            mFragmentTabAdapter?.loadStateFlow?.collect {
                Log.d(TAG, "loadStateFlow: $it")
            }
        }


        /*mProjectChaptersViewModel!!.mutableLiveData.observe(
            this,
            Observer<ProjectChaptersBean> { projectGroupData ->
                if (projectGroupData?.projectChapters == null) {
                    showErrorView()
                } else {
                    projectsAdapter.setProjectData(projectGroupData.projectChapters)
                    showSuccessView()
                }
            })
        mTabLayout = contentView.findViewById(R.id.tab_layout)
        mTabLayout?.setupWithViewPager(mViewPager)*/
    }

    override fun initData() {
        showLoadingView()
        //mFragmentTabAdapter?.refresh(Unit)
        mProjectChaptersViewModel?.flow?.let {
            mFragmentTabAdapter?.submitData(it)
        }
    }

    private fun showLoadingView() {
    }

    private fun showSuccessView() {
    }

    private fun showErrorView() {

    }
}

sealed class ProjectStore<Value> : IStore<Value> {

    data class Num(override val value: Int) : ProjectStore<Int>()

}

