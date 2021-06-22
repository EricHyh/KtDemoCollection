package com.hyh.tabs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.PagerAdapter
import com.hyh.fragment.BaseFragment
import com.hyh.page.PageContext
import com.hyh.tabs.AbsViewTab
import com.hyh.tabs.FragmentTab
import com.hyh.tabs.LoadState
import com.hyh.tabs.TabInfo
import com.hyh.tabs.internal.TabData
import kotlinx.coroutines.flow.Flow
import java.lang.NullPointerException
import kotlin.collections.set

/**
 * [AbsViewTab]的[PagerAdapter]实现
 *
 * @author eriche
 * @data 2021/5/20
 */

class ViewTabAdapter<Param : Any>(private val pageContext: PageContext) : PagerAdapter(), ITabAdapter<Param, AbsViewTab> {

    private val baseTabAdapter: BaseTabAdapter<Param, AbsViewTab> = object : BaseTabAdapter<Param, AbsViewTab>(pageContext) {
        override fun notifyDataSetChanged() {
            this@ViewTabAdapter.notifyDataSetChanged()
        }

        override val currentPrimaryItem: AbsViewTab?
            get() = this@ViewTabAdapter.currentPrimaryItem

    }

    private val attachedTabs: MutableList<TabRecord> = mutableListOf()

    private val tabCacheMap: MutableMap<TabInfo<AbsViewTab>, TabRecord> = mutableMapOf()

    private var _currentPrimaryItem: AbsViewTab? = null
    override val currentPrimaryItem: AbsViewTab?
        get() = _currentPrimaryItem
    override val tabCount: Int
        get() = baseTabAdapter.tabCount
    override val tabTokens: List<Any>?
        get() = baseTabAdapter.tabTokens
    override val tabTitles: List<CharSequence>?
        get() = baseTabAdapter.tabTitles
    override val loadStateFlow: Flow<LoadState>
        get() = baseTabAdapter.loadStateFlow


    override fun submitData(flow: Flow<TabData<Param, AbsViewTab>>) {
        baseTabAdapter.submitData(flow)
    }

    override fun refresh(param: Param) {
        baseTabAdapter.refresh(param)
    }

    override fun getCount(): Int = baseTabAdapter.tabCount


    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        bindParentFragmentLifeCycle()

        val tabInfo = baseTabAdapter.getTabInfo(position) ?: throw NullPointerException()

        //使用缓存
        var tabRecord = tabCacheMap.remove(tabInfo)
        if (tabRecord != null) {
            val cacheTab = tabRecord.tab
            val needCache = tabInfo.isTabNeedCache
            tabRecord = TabRecord(tabInfo, position, needCache, cacheTab)

            if (needCache) {
                tabCacheMap[tabInfo] = tabRecord
            }

            val view = cacheTab.performCreateView(LayoutInflater.from(container.context), container)
            container.addView(view)
            cacheTab.performViewCreated(view)

            return tabRecord.apply {
                attachedTabs.add(this)
            }
        }

        //新建Tab
        val tab = tabInfo.lazyTab.value
        val needCache = tabInfo.isTabNeedCache
        tabRecord = TabRecord(tabInfo, position, needCache, tab)

        if (needCache) {
            tabCacheMap[tabInfo] = tabRecord
        }

        tab.performCreate()
        val view = tab.performCreateView(LayoutInflater.from(container.context), container)
        container.addView(view)
        tab.performViewCreated(view)

        return tabRecord.apply {
            attachedTabs.add(this)
        }
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        val tabRecord = `object` as? TabRecord
        val absViewTab = tabRecord?.tab

        val tempCurrentPrimaryItem = _currentPrimaryItem
        if (absViewTab != tempCurrentPrimaryItem) {

            if (tempCurrentPrimaryItem != null) {
                if (tempCurrentPrimaryItem.isVisible) {
                    tempCurrentPrimaryItem.performTabInvisible()
                }
            }

            if (absViewTab?.isVisible != true) {
                if (pageContext.lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    absViewTab?.performTabVisible()
                }
            }

            _currentPrimaryItem = absViewTab
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val tabRecord = `object` as? TabRecord
        attachedTabs.remove(tabRecord)

        val absViewTab = tabRecord?.tab
        absViewTab?.view?.let {
            val parent = it.parent as? ViewGroup
            parent?.removeView(it)
        }
        absViewTab?.performDestroyView()
        if (tabRecord?.cached != true) {
            absViewTab?.performDestroy()
        }

        if (absViewTab == _currentPrimaryItem) {
            _currentPrimaryItem = null
        }
    }

    override fun getPageTitle(position: Int): CharSequence? = baseTabAdapter.getTabInfo(position)?.tabTitle

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return (`object` as? TabRecord)?.tab?.view == view
    }

    override fun getItemPosition(`object`: Any): Int {
        val tabRecord = `object` as? TabRecord ?: return POSITION_NONE
        val currentPosition = baseTabAdapter.indexOf(tabRecord.tabInfo)
        if (currentPosition < 0 || currentPosition >= baseTabAdapter.tabCount) {
            return POSITION_NONE
        }
        return currentPosition
    }

    private fun bindParentFragmentLifeCycle() {
        /*if (pageContext.lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }
        if (parentLifecycleObserver.isInitialized()) {
            return
        }
        parentLifecycleOwner.lifecycle.addObserver(parentLifecycleObserver.value)*/
    }

    private inner class ParentFragmentLifecycleObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_RESUME -> dispatchOnTabVisible()
                Lifecycle.Event.ON_PAUSE -> dispatchOnTabInvisible()
                Lifecycle.Event.ON_DESTROY -> dispatchOnDestroy()
                else -> {
                }
            }
        }

        private fun dispatchOnTabVisible() {
            _currentPrimaryItem?.let {
                if (!it.isVisible) {
                    it.performTabVisible()
                }
            }
        }

        private fun dispatchOnTabInvisible() {
            _currentPrimaryItem?.let {
                if (it.isVisible) {
                    it.performTabInvisible()
                }
            }
        }

        private fun dispatchOnDestroy() {
            attachedTabs.forEach {
                val absViewTab = it.tab
                if (absViewTab.isVisible) {
                    absViewTab.performTabInvisible()
                }
                absViewTab.performDestroyView()
                absViewTab.performDestroy()
            }
            tabCacheMap.values.filter { !attachedTabs.contains(it) }
                .forEach {
                    val absViewTab = it.tab
                    absViewTab.performDestroy()
                }
            attachedTabs.clear()
            tabCacheMap.clear()
        }
    }

    data class TabRecord(
        val tabInfo: TabInfo<AbsViewTab>,
        val position: Int,
        val cached: Boolean,
        val tab: AbsViewTab
    )
}