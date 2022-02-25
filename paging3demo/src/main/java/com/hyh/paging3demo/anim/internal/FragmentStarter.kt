package com.hyh.paging3demo.anim.internal

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle


/**
 * TODO: Add Description
 *
 * @author eriche 2022/1/13
 */
class FragmentStarter(
    private val fragmentManager: IFragmentManager
) {

    fun from(fragment: Fragment): StarterBuilder {
        return StarterBuilder(fragment)
    }

    inner class StarterBuilder constructor(private val fragment: Fragment) {

        private var clazz: Class<out Fragment>? = null
        private var args: Bundle? = null
        private var tag: String? = null

        fun target(clazz: Class<out Fragment>): StarterBuilder {
            this.clazz = clazz
            return this
        }

        fun args(args: Bundle): StarterBuilder {
            this.args = args
            return this
        }

        fun tag(tag: String): StarterBuilder {
            this.tag = tag
            return this
        }

        /*fun start() {
            val clazz = this.clazz ?: return
            if (fragment.lifecycle.currentState < Lifecycle.State.CREATED) return
            val newInstance = clazz.newInstance()
            fragmentManager.add(newInstance, args, tag)
            FragmentAnimFactory
                .getAnimOut(fragment)
                .start {
                    FragmentAnimFactory.getAnimIn(newInstance).start { }
                }
        }*/
    }
}