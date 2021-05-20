package com.hyh.tabs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.PagerAdapter
import com.hyh.tabs.AbsViewTab

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */

class ViewTabAdapter<Key : Any>(private val lifecycleOwner: LifecycleOwner) : PagerAdapter() {

    private val tabDataHandler: TabDataHandler<Key, AbsViewTab> = TabDataHandler()

    private val tabRecordMap: MutableMap<View, TabRecord> = mutableMapOf()

    private val mCurrentPrimaryItem: AbsViewTab? = null

    override fun getCount(): Int =
        if (lifecycleOwner.lifecycle.currentState <= Lifecycle.State.DESTROYED) 0 else tabDataHandler.tabCount


    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val tab = tabDataHandler.getTab(position)
        tab.performCreate()
        val view = tab.performCreateView(LayoutInflater.from(container.context), container)
        tabRecordMap[view] = TabRecord(tab, position, view)
        container.addView(view)
        tab.performViewCreated(view)
        return tab
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)

    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val absViewTab = `object` as? AbsViewTab
        absViewTab?.view?.let {
            val parent = it.parent as? ViewGroup
            parent?.removeView(it)
        }
        absViewTab?.performDestroyView()
    }

    override fun getPageTitle(position: Int): CharSequence? =
        tabDataHandler.getTab(position).getTabTitle()

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return (`object` as? AbsViewTab)?.view == view
    }

    override fun getItemPosition(`object`: Any): Int {
        return super.getItemPosition(`object`)
    }

    data class TabRecord(val tab: AbsViewTab, val position: Int, val view: View)

}