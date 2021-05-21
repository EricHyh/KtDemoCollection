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
import com.hyh.tabs.AbsViewTab
import kotlin.collections.set

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */

class ViewTabAdapter<Param : Any> : PagerAdapter {

    private val parentLifecycleOwner: LifecycleOwner

    private val parentLifecycleObserver: Lazy<LifecycleObserver> = lazy {
        ParentFragmentLifecycleObserver()
    }

    private val tabDataHandler: TabDataHandler<Param, AbsViewTab> = TabDataHandler()

    private val attachedTabs: MutableList<TabRecord> = mutableListOf()

    private val tabCacheMap: MutableMap<Any, TabRecord> = mutableMapOf()

    private var currentPrimaryItem: AbsViewTab? = null

    constructor(parentFragment: BaseFragment) {
        this.parentLifecycleOwner = parentFragment
    }

    constructor(parentTab: AbsViewTab) {
        this.parentLifecycleOwner = parentTab
    }

    override fun getCount(): Int =
        if (parentLifecycleOwner.lifecycle.currentState <= Lifecycle.State.DESTROYED) 0 else tabDataHandler.tabCount


    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        bindParentFragmentLifeCycle()

        val tabToken = tabDataHandler.getTabFactory().getTabToken(position)

        //使用缓存
        var tabRecord = tabCacheMap.remove(tabToken)
        if (tabRecord != null) {
            val cacheTab = tabRecord.tab
            val needCache = tabDataHandler.getTabFactory().isTabNeedCache(position)
            tabRecord = TabRecord(tabToken, position, needCache, cacheTab)

            if (needCache) {
                tabCacheMap[tabToken] = tabRecord
            }

            val view = cacheTab.performCreateView(LayoutInflater.from(container.context), container)
            container.addView(view)
            cacheTab.performViewCreated(view)

            return tabRecord.apply {
                attachedTabs.add(this)
            }
        }

        //新建Tab
        val tab = tabDataHandler.getTabFactory().createTab(position)
        val needCache = tabDataHandler.getTabFactory().isTabNeedCache(position)
        tabRecord = TabRecord(tabToken, position, needCache, tab)

        if (needCache) {
            tabCacheMap[tabToken] = tabRecord
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

        val tempCurrentPrimaryItem = currentPrimaryItem
        if (absViewTab != tempCurrentPrimaryItem) {

            if (tempCurrentPrimaryItem != null) {
                if (tempCurrentPrimaryItem.isVisible) {
                    tempCurrentPrimaryItem.performTabInvisible()
                }
            }

            if (absViewTab?.isVisible != true) {
                if (parentLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    absViewTab?.performTabVisible()
                }
            }

            currentPrimaryItem = absViewTab
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

        if (absViewTab == currentPrimaryItem) {
            currentPrimaryItem = null
        }
    }

    override fun getPageTitle(position: Int): CharSequence? =
        tabDataHandler.getTabFactory().getTabTitle(position)

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return (`object` as? TabRecord)?.tab?.view == view
    }

    override fun getItemPosition(`object`: Any): Int {
        val tabRecord = `object` as? TabRecord ?: return POSITION_NONE
        val currentPosition = tabDataHandler.getTabFactory().getCurrentPosition(tabRecord.tabToken)
        if (currentPosition < 0 || currentPosition >= tabDataHandler.tabCount) {
            return POSITION_NONE
        }
        return currentPosition
    }

    private fun bindParentFragmentLifeCycle() {
        if (parentLifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }
        if (parentLifecycleObserver.isInitialized()) {
            return
        }
        parentLifecycleOwner.lifecycle.addObserver(parentLifecycleObserver.value)
    }

    inner class ParentFragmentLifecycleObserver : LifecycleEventObserver {

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
            currentPrimaryItem?.let {
                if (!it.isVisible) {
                    currentPrimaryItem?.performTabVisible()
                }
            }
        }

        private fun dispatchOnTabInvisible() {
            currentPrimaryItem?.let {
                if (it.isVisible) {
                    currentPrimaryItem?.performTabInvisible()
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
        val tabToken: Any,
        val position: Int,
        val cached: Boolean,
        val tab: AbsViewTab
    )
}