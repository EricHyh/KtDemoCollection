package com.example.ndkdemo

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.ndk_demo_lib.TestCallback
import com.example.ndk_demo_lib.TestJNI
import com.example.ndk_demo_lib.TestSwig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.tv_content)
        val testJNI = TestJNI()
        val testSwig = TestSwig(1.2)
        textView.text = testJNI.stringFromJNI() + testSwig.Area()

        testSwig.testCallback(object :TestCallback(){

        })

        TestGc()

        Handler().postDelayed({
            System.gc()
        }, 1000L)
    }

}

class TestGc {

    protected fun finalize() {
        Log.d("TestGc", "finalize: ")
    }

}