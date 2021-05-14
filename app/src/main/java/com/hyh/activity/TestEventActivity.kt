package com.hyh.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hyh.demo.R
import com.hyh.event.IEvent.Companion.asEvent
import com.hyh.event.IEvent.Companion.unwrapData
import com.hyh.event.IEventChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.Executors

class TestEventActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TestEventActivity_"
    }

    private val mEventChannel = IEventChannel.Factory.create(this)

    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_event)
        GlobalScope
            .launch {
                mEventChannel.getFlow()
                    .collect {
                        Log.d(TAG, "onCreate1: ${it.unwrapData<Int>()}")
                    }
            }
        lifecycleScope
            .launch {
                mEventChannel.getFlow()
                    .collect {
                        Log.d(TAG, "onCreate2: ${it.unwrapData<Int>()}")
                    }
            }
        val subscribe = mEventChannel.getObservable()
            .subscribe {
                Log.d(TAG, "onCreate2 - 1: ${it.unwrapData<Int>()}")
            }
    }

    fun test1(view: View) {
        mEventChannel.send(20.asEvent())
        mEventChannel.send(21.asEvent())
    }


    fun test2(view: View) {
        lifecycleScope
            .launch {
                mEventChannel.getFlow()
                    .collect {
                        Log.d(TAG, "test2 - 1: ${it.unwrapData<Int>()}")
                    }
            }
        mEventChannel.send(50.asEvent())
        mEventChannel.send(51.asEvent())
        lifecycleScope
            .launch {
                mEventChannel.getFlow()
                    .collect {
                        Log.d(TAG, "test2 -2: ${it.unwrapData<Int>()}")
                    }
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        mEventChannel.send(30.asEvent())
        mEventChannel.send(31.asEvent())
        mEventChannel.send(32.asEvent())
        mEventChannel.send(33.asEvent())
    }
}