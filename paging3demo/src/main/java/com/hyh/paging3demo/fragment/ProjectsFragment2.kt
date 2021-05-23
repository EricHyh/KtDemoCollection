package com.hyh.paging3demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.adapter.ProjectsAdapter
import com.hyh.paging3demo.bean.ProjectChaptersBean
import com.hyh.paging3demo.viewmodel.ContextViewModelFactory
import com.hyh.paging3demo.viewmodel.ProjectChaptersViewModel
import com.hyh.paging3demo.viewmodel.ProjectChaptersViewModel2
import com.hyh.tabs.adapter.FragmentTabAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProjectsFragment2 : CommonBaseFragment() {

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

        mFragmentTabAdapter = FragmentTabAdapter<Unit>(childFragmentManager)
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_projects2, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


    override fun initView(contentView: View) {
        contentView.findViewById<Button>(R.id.btn_refresh)
            .setOnClickListener {
                mFragmentTabAdapter?.refresh(Unit)
            }


        mViewPager = contentView.findViewById<ViewPager>(R.id.view_pager)
        mViewPager!!.adapter = mFragmentTabAdapter
        mViewPager!!.offscreenPageLimit = 1




        mTabLayout = contentView.findViewById(R.id.tab_layout)
        mTabLayout?.setupWithViewPager(mViewPager)

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

        lifecycleScope.launch {
            mProjectChaptersViewModel?.flow?.collectLatest {
                mFragmentTabAdapter?.submitData(it)
            }
        }
    }

    private fun showLoadingView() {
    }

    private fun showSuccessView() {
    }

    private fun showErrorView() {

    }
}