package com.hyh.lifecycle

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.hyh.demo.R

class TestLifecycleActivity : AppCompatActivity() {

    private val TAG = "TestLifecycleActivity_"

    private val lifecycle: LifecycleRegistry by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleRegistry(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_lifecycle)
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "onStateChanged: $event")
            }
        })
    }


    fun lifecycleOnCreate(v: View) {
        //lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycle.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "lifecycleOnCreate: ${lifecycle.currentState}")
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
}