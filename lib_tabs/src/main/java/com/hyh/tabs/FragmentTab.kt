package com.hyh.tabs

import androidx.fragment.app.Fragment

/**
 * Fragment Tab
 *
 * @author eriche
 * @data 2021/5/20
 */
open class FragmentTab(val fragment: Fragment) : ITab {

    override fun hashCode(): Int {
        return fragment.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FragmentTab) return false
        return fragment == other.fragment
    }
}