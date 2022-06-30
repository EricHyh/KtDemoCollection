package com.hyh.lifecycle

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel


/**
 * TODO: Add Description
 *
 * @author eriche 2022/6/29
 */
class LifecycleViewModel : ViewModel(), LifecycleOwner {

    companion object {
        private const val TAG = "LifecycleViewModel"
    }

    @SuppressLint("StaticFieldLeak")
    private val _lifecycleRegistry: LifecycleRegistry = ChildLifecycleRegistry(this)

    init {
        _lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }


    /**
     * Returns the Lifecycle of the provider.
     *
     * @return The lifecycle of the provider.
     */
    override fun getLifecycle(): Lifecycle {
        return _lifecycleRegistry
    }

    override fun onCleared() {
        _lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}