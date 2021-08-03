package com.hyh.tabs.adapter

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.hyh.page.PageContext
import com.hyh.tabs.FragmentTab
import com.hyh.tabs.LoadState
import com.hyh.tabs.TabInfo
import com.hyh.tabs.internal.TabData
import kotlinx.coroutines.flow.Flow
import java.lang.NullPointerException
import java.util.concurrent.atomic.AtomicLong

/**
 * [FragmentTab]的[PagerAdapter]实现
 *
 * @author eriche
 * @data 2021/5/21
 */
class FragmentTabAdapter<Param : Any>(
    pageContext: PageContext,
    fm: FragmentManager
) : FragmentPagerAdapter(fm), ITabAdapter<Param, FragmentTab> {

    companion object {
        private const val TAG = "FragmentTabAdapter"
    }

    private val baseTabAdapter: BaseTabAdapter<Param, FragmentTab> = object : BaseTabAdapter<Param, FragmentTab>(pageContext) {
        override fun notifyDataSetChanged() {
            this@FragmentTabAdapter.notifyDataSetChanged()
        }

        override val currentPrimaryItem: FragmentTab?
            get() = this@FragmentTabAdapter.currentPrimaryItem
    }

    private var _currentPrimaryItem: FragmentTab? = null
    override val currentPrimaryItem: FragmentTab?
        get() = null
    override val tabCount: Int
        get() = baseTabAdapter.tabCount
    override val tabTokens: List<Any>?
        get() = baseTabAdapter.tabTokens
    override val tabTitles: List<CharSequence>?
        get() = baseTabAdapter.tabTitles
    override val loadStateFlow: Flow<LoadState>
        get() = baseTabAdapter.loadStateFlow

    private val itemIdAtomicLong: AtomicLong = AtomicLong(0)

    private val itemIdMap: MutableMap<Any, Long> = mutableMapOf()

    private val tabRecordMap: MutableMap<Int, TabInfo<FragmentTab>> = mutableMapOf()

    override fun getItem(position: Int): Fragment {
        val tabInfo = baseTabAdapter.getTabInfo(position) ?: throw NullPointerException()
        val fragment = tabInfo.lazyTab.value.fragment
        tabRecordMap[fragment.hashCode()] = tabInfo
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        val fragment = `object` as? Fragment ?: return
        tabRecordMap.remove(fragment.hashCode())
    }

    override fun getCount(): Int = baseTabAdapter.tabCount

    override fun getItemId(position: Int): Long {
        val tabInfo = baseTabAdapter.getTabInfo(position) ?: return -1
        var id = itemIdMap[tabInfo.tabToken]
        if (id == null) {
            id = itemIdAtomicLong.incrementAndGet()
            itemIdMap[tabInfo.tabToken] = id
        }
        return id
    }

    override fun getItemPosition(`object`: Any): Int {
        val fragment = `object` as? Fragment ?: return PagerAdapter.POSITION_NONE
        val tabInfo: TabInfo<FragmentTab> = tabRecordMap[fragment.hashCode()] ?: return PagerAdapter.POSITION_NONE
        val currentPosition = baseTabAdapter.indexOf(tabInfo)
        if (currentPosition < 0 || currentPosition >= baseTabAdapter.tabCount) {
            return PagerAdapter.POSITION_NONE
        }
        return currentPosition
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return baseTabAdapter.getTabTitle(position)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        val fragment = `object` as Fragment
        _currentPrimaryItem = FragmentTab(fragment)
    }

    override fun submitData(flow: Flow<TabData<Param, FragmentTab>>) {
        baseTabAdapter.submitData(flow)
    }

    override fun refresh(param: Param) {
        baseTabAdapter.refresh(param)
    }
}