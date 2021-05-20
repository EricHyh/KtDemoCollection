package com.hyh.tabs

import androidx.fragment.app.Fragment

/**
 * Fragment Tab 基类
 *
 * @author eriche
 * @data 2021/5/20
 */
abstract class AbsFragmentTab : ITab {

    companion object {
        private const val TAG = "AbsFragmentTab"
    }

    abstract fun getFragment(): Fragment
}