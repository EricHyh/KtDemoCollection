package com.hyh.tabs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/1/29
 */
abstract class TabsFragment : Fragment() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    abstract fun getViewPager(): ViewPager



}