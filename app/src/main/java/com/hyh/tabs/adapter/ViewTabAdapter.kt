package com.hyh.tabs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import com.hyh.tabs.AbsViewTab

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */

class ViewTabAdapter<Key : Any> : PagerAdapter() {

    private val tabDataHandler: TabDataHandler<Key, AbsViewTab> = TabDataHandler()

    private val tabRecordMap: MutableMap<View, TabRecord> = mutableMapOf()

    private val mCurrentPrimaryItem: AbsViewTab? = null

    override fun getCount(): Int = tabDataHandler.tabCount

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

    }

    override fun getPageTitle(position: Int): CharSequence? =
        tabDataHandler.getTab(position).getTabTitle()

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        TODO("Not yet implemented")
    }

    override fun getItemPosition(`object`: Any): Int {
        return super.getItemPosition(`object`)
    }

    data class TabRecord(val tab: AbsViewTab, val position: Int, val view: View)

}