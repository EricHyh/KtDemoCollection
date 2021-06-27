package com.hyh.tabs

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.ViewPager
import com.hyh.page.PageContext
import com.hyh.tabs.adapter.FragmentTabAdapter
import com.hyh.tabs.adapter.ITabAdapter

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/24
 */
class BaseFragmentTabsWidget<Param : Any>(pageContext: PageContext, fm: FragmentManager) {

    private var tabSource: TabSource<Param, FragmentTab>? = null
    private var tabsAdapter: FragmentTabAdapter<Param> = FragmentTabAdapter(pageContext, fm)

    fun setup(lifecycleOwner: LifecycleOwner, viewPager: ViewPager) {
        viewPager.adapter = tabsAdapter
    }


    fun refresh(param: Param) {
        tabsAdapter.refresh(param)
    }
}