package com.hyh.lifecycle

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry


interface IChildLifecycleOwner {

    val lifecycleOwner: ChildLifecycleOwner

    fun bindParentLifecycle(lifecycle: Lifecycle) {
        lifecycleOwner.parentLifecycle = lifecycle
    }
}


fun Lifecycle.createChildLifecycleOwner(): ChildLifecycleOwner {
    return ChildLifecycleOwner(this)
}

class ChildLifecycleOwner(lifecycle: Lifecycle? = null) : LifecycleOwner {

    var parentLifecycle: Lifecycle? = lifecycle
        set(value) {
            field = value
            childLifecycle.parentLifecycle = value
        }
        get() {
            Log.d("ChildLifecycleOwner", "get: $field")
            return field
        }

    private val childLifecycle: ChildLifecycleRegistry = ChildLifecycleRegistry(this).apply {
        this.parentLifecycle = this@ChildLifecycleOwner.parentLifecycle
    }

    override fun getLifecycle(): ChildLifecycleRegistry {
        return childLifecycle
    }
}

class ChildLifecycleRegistry(owner: LifecycleOwner) : LifecycleRegistry(owner) {

    private val parentLifecycleEventObserver = ParentLifecycleEventObserver()

    var parentLifecycle: Lifecycle? = null
        set(value) {
            val oldValue = field
            field = value
            if (oldValue != value) {
                oldValue?.removeObserver(parentLifecycleEventObserver)
                value?.addObserver(parentLifecycleEventObserver)
                updateState(value)
            }
        }
        get() {
            Log.d("ChildLifecycleRegistry", "get parentLifecycle")
            return field
        }

    private var _selfState = State.INITIALIZED

    val selfState
        get() = _selfState

    override fun setCurrentState(state: State) {
        this._selfState = state
        updateState(parentLifecycle)
    }


    private fun updateState(parentLifecycle: Lifecycle?) {

        Log.d("updateState", "$parentLifecycle")

        fun getParentCurrentState(): State {
            return parentLifecycle?.currentState ?: State.RESUMED
        }

        val childState = currentState
        val resultState = _selfState.coerceAtMost(getParentCurrentState())
        if (childState == State.INITIALIZED && resultState == State.DESTROYED) {
            return
        }
        if (resultState == State.INITIALIZED) {
            return
        }
        super.setCurrentState(resultState)
    }

    inner class ParentLifecycleEventObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Event) {
            updateState(source.lifecycle)
        }
    }
}