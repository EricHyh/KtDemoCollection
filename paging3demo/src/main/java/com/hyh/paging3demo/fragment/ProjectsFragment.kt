package com.hyh.paging3demo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.adapter.ProjectsAdapter
import com.hyh.paging3demo.bean.ProjectCategoriesBean
import com.hyh.paging3demo.viewmodel.ContextViewModelFactory
import com.hyh.paging3demo.viewmodel.ProjectCategoriesViewModel

class ProjectsFragment : CommonBaseFragment() {

    private var mProjectCategoriesViewModel: ProjectCategoriesViewModel? = null

    private var mTabLayout: TabLayout? = null

    private var mViewPager: ViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProjectCategoriesViewModel = ViewModelProvider(
            this,
            ContextViewModelFactory(context!!)
        ).get(ProjectCategoriesViewModel::class.java)
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    override fun initView(contentView: View) {
        val projectsAdapter = ProjectsAdapter(contentView.context, childFragmentManager)
        mViewPager = contentView.findViewById<ViewPager>(R.id.view_pager)
        mViewPager!!.setAdapter(projectsAdapter)
        mViewPager!!.setOffscreenPageLimit(1)

        mProjectCategoriesViewModel!!.mutableLiveData.observe(
            this,
            Observer<ProjectCategoriesBean> { projectGroupData ->
                if (projectGroupData?.projectCategories == null) {
                    showErrorView()
                } else {
                    projectsAdapter.setProjectData(projectGroupData.projectCategories)
                    showSuccessView()
                }
            })
        mTabLayout = contentView.findViewById(R.id.tab_layout)
        mTabLayout?.setupWithViewPager(mViewPager)
    }

    override fun initData() {
        showLoadingView()
        mProjectCategoriesViewModel!!.loadData()
    }

    private fun showLoadingView() {
    }

    private fun showSuccessView() {
    }

    private fun showErrorView() {
    }
}