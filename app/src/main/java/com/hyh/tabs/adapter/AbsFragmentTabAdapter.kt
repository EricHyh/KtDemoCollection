package com.hyh.tabs.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.hyh.tabs.AbsFragmentTab
import com.hyh.tabs.internal.TabData

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
class AbsFragmentTabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    companion object {
        private const val TAG = "AbsFragmentTabAdapter"
    }

    override fun getItem(position: Int): Fragment {
        TODO("Not yet implemented")
    }

    override fun getCount(): Int {
        TODO("Not yet implemented")
    }


}