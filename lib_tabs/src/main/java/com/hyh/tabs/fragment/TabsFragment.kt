package com.hyh.tabs.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.hyh.tabs.AbsViewTab
import com.hyh.tabs.TabInfo

/**
 * 多 Tab Fragment
 *
 * @author eriche
 * @data 2021/1/29
 */
abstract class TabsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*TabInfo(
            lazyTab = lazy {
                object : AbsViewTab() {
                    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup): View {
                        TODO("Not yet implemented")
                    }
                }
            },
            tabToken = "",
            tabTitle = "",
        )*/
    }

    abstract fun getViewPager(): ViewPager


}