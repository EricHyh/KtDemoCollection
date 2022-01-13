package com.hyh.paging3demo.anim.internal

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * TODO: Add Description
 *
 * @author eriche 2022/1/13
 */
interface IFragmentManager {

    fun add(fragment: Fragment, args: Bundle?, tag: String?)

    fun remove(fragment: Fragment)

}