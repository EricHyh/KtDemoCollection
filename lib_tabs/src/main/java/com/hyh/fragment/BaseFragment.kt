package com.hyh.fragment

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.hyh.tabs.adapter.ViewTabAdapter

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/21
 */
class BaseFragment : Fragment() {
    companion object {
        private const val TAG = "BaseFragment"
    }

    private val mViewLifecycle by lazy {
        LifecycleRegistry(this@BaseFragment)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewLifecycle.currentState = Lifecycle.State.STARTED
    }

    @CallSuper
    fun onSupportVisible() {
        mViewLifecycle.currentState = Lifecycle.State.RESUMED
    }

    @CallSuper
    fun onSupportInvisible() {
        mViewLifecycle.currentState = Lifecycle.State.STARTED
    }


    @CallSuper
    override fun onDestroyView() {
        mViewLifecycle.currentState = Lifecycle.State.DESTROYED
        super.onDestroyView()
    }

    fun getLifecycleForView(): LifecycleRegistry {
        return mViewLifecycle
    }

    fun getLifecycleOwnerForView(): LifecycleOwner {
        return LifecycleOwner { mViewLifecycle}
    }

    override fun getViewLifecycleOwner(): LifecycleOwner {
        return super.getViewLifecycleOwner()
    }

    override fun getLifecycle(): Lifecycle {
        return super.getLifecycle()
    }


}