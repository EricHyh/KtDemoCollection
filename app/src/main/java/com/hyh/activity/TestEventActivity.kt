package com.hyh.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hyh.demo.R
import com.hyh.event.IEvent.Companion.asEvent
import com.hyh.event.IEventChannel
import com.hyh.event.unwrapData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
    }

    fun test(view: View) {
        mEventChannel.send(20.asEvent())
        mEventChannel.send(21.asEvent())
    }

    override fun onDestroy() {
        super.onDestroy()
        mEventChannel.send(30.asEvent())
        mEventChannel.send(31.asEvent())
        mEventChannel.send(32.asEvent())
        mEventChannel.send(33.asEvent())
    }
}