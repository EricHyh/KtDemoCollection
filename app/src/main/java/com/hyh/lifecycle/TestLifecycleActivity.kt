package com.hyh.lifecycle

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.hyh.demo.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class TestLifecycleActivity : AppCompatActivity() {

    private val TAG = "TestLifecycleActivity_"

    private val parentLifecycle: LifecycleRegistry = LifecycleRegistry(this)

    private val lifecycleOwner = parentLifecycle.createChildLifecycleOwner()

    private val lifecycle: LifecycleRegistry by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        lifecycleOwner.lifecycle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_lifecycle)
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "onStateChanged: $event")
            }
        })

        parentLifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "parent onStateChanged: $event")
            }
        })
    }


    fun lifecycleOnCreate(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycle.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "lifecycleOnCreate: ${lifecycle.currentState}")



        Observable.fromIterable(listOf(0, 1, 2, 3))
            .flatMap { int1 ->
                Observable.range(0, 5)
                    .observeOn(Schedulers.io())
                    .map { int2 ->
                        val result = "$int1 - $int2"
                        Log.d(TAG, "map ${Thread.currentThread().name} - $result")
                        result
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        Log.d(TAG, "doOnNext ${Thread.currentThread().name} - $it")
                    }
            }
            .subscribe()


    }

    fun lifecycleOnStart(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.currentState = Lifecycle.State.STARTED
        Log.d(TAG, "lifecycleOnStart: ${lifecycle.currentState}")
    }


    fun lifecycleOnResume(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        lifecycle.currentState = Lifecycle.State.RESUMED
        Log.d(TAG, "lifecycleOnResume: ${lifecycle.currentState}")
    }


    fun lifecycleOnPause(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.currentState = Lifecycle.State.STARTED
        Log.d(TAG, "lifecycleOnPause: ${lifecycle.currentState}")
    }

    fun lifecycleOnStop(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycle.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "lifecycleOnStop: ${lifecycle.currentState}")
    }

    fun lifecycleOnDestroy(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.currentState = Lifecycle.State.DESTROYED
        Log.d(TAG, "lifecycleOnDestroy: ${lifecycle.currentState}")
    }


    fun parentLifecycleOnCreate(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        parentLifecycle.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "lifecycleOnCreate: ${lifecycle.currentState}")
    }

    fun parentLifecycleOnStart(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        parentLifecycle.currentState = Lifecycle.State.STARTED
        Log.d(TAG, "lifecycleOnStart: ${lifecycle.currentState}")
    }

    fun parentLifecycleOnResume(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        parentLifecycle.currentState = Lifecycle.State.RESUMED
        Log.d(TAG, "lifecycleOnResume: ${lifecycle.currentState}")
    }


    fun parentLifecycleOnPause(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        parentLifecycle.currentState = Lifecycle.State.STARTED
        Log.d(TAG, "lifecycleOnPause: ${lifecycle.currentState}")
    }

    fun parentLifecycleOnStop(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        parentLifecycle.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "lifecycleOnStop: ${lifecycle.currentState}")
    }

    fun parentLifecycleOnDestroy(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        parentLifecycle.currentState = Lifecycle.State.DESTROYED
        Log.d(TAG, "lifecycleOnDestroy: ${lifecycle.currentState}")
    }
}


fun Lifecycle.createChildLifecycleOwner(): ChildLifecycleOwner {
    return ChildLifecycleOwner(this)
}

class ChildLifecycleOwner(private val parentLifecycle: Lifecycle) : LifecycleOwner {

    private val childLifecycle: LifecycleRegistry by lazy {
        ChildLifecycleRegistry(parentLifecycle, this)
    }

    override fun getLifecycle(): LifecycleRegistry {
        return childLifecycle
    }
}

class ChildLifecycleRegistry(private val parentLifecycle: Lifecycle, owner: LifecycleOwner) : LifecycleRegistry(owner) {

    init {
        parentLifecycle.addObserver(ParentLifecycleEventObserver())
    }

    private var selfState = State.INITIALIZED

    override fun setCurrentState(state: State) {
        this.selfState = state
        val resultState = state.coerceAtMost(parentLifecycle.currentState)
        if (resultState == State.DESTROYED
            && currentState == State.INITIALIZED
        ) {
            return
        }
        super.setCurrentState(resultState)
    }

    private fun superSetCurrentState(state: State) {
        super.setCurrentState(state)
    }

    inner class ParentLifecycleEventObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Event) {
            val childState = currentState
            val parentState = parentLifecycle.currentState
            val resultState = selfState.coerceAtMost(parentState)
            if (childState == State.INITIALIZED && resultState == State.DESTROYED) {
                return
            }
            superSetCurrentState(resultState)
        }
    }
}