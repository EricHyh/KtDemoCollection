package com.hyh.paging3demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.adapter.ProjectsAdapter
import com.hyh.paging3demo.bean.ProjectChaptersBean
import com.hyh.paging3demo.viewmodel.ContextViewModelFactory
import com.hyh.paging3demo.viewmodel.ProjectChaptersViewModel

class ProjectsFragment : CommonBaseFragment() {

    private var mProjectChaptersViewModel: ProjectChaptersViewModel? = null

    private var mTabLayout: TabLayout? = null

    private var mViewPager: ViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProjectChaptersViewModel = ViewModelProvider(
            this,
            ContextViewModelFactory(context!!)
        ).get(ProjectChaptersViewModel::class.java)
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        setRetainInstance
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    companion object {
        private const val DIFFICULTY_LOW = 0
        private const val DIFFICULTY_MEDIUM = 1
        private const val DIFFICULTY_HIGH = 1

        const val DATA_TYPE = 1
        val DATA_KEY = "data_key"

        private var sHasData: Boolean? = null
    }

    private fun doSomething(difficulty: Int) {
        when (difficulty) {
            DIFFICULTY_LOW -> {
            }
            DIFFICULTY_MEDIUM -> {
            }
            DIFFICULTY_HIGH -> {
            }
            else -> {
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val flag = 1
        when (flag) {
            DIFFICULTY_LOW -> {
            }
            DIFFICULTY_MEDIUM -> {
            }
            DIFFICULTY_HIGH -> {
            }
            else -> {
            }
        }
    }

    fun doSomething(
        params1: Any,
        params2: Any,
        params3: Any,
        params4: Any
    ) {

    }

    override fun initView(contentView: View) {
        val projectsAdapter = ProjectsAdapter(contentView.context, childFragmentManager)
        mViewPager = contentView.findViewById<ViewPager>(R.id.view_pager)
        mViewPager!!.setAdapter(projectsAdapter)
        mViewPager!!.setOffscreenPageLimit(1)

        mProjectChaptersViewModel!!.mutableLiveData.observe(
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
        mTabLayout?.setupWithViewPager(mViewPager)

        doSomething(
            params1 = "",
            params2 = "",
            params3 = "",
            params4 = ""
        )
    }

    override fun initData() {
        showLoadingView()
        mProjectChaptersViewModel!!.loadData()
    }

    private fun showLoadingView() {
    }

    private fun showSuccessView() {
    }

    private fun showErrorView() {

    }
}