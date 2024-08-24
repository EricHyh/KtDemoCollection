package com.example.ndkdemo

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.ndk_demo_lib.LegacyLibrary.LegacyClass
import com.example.ndk_demo_lib.TestJNI
import com.example.ndk_demo_lib.TestSwig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.tv_content)
        val testJNI = TestJNI<Int>()
        val testSwig = TestSwig(1.2)
        textView.text = "" + testJNI.stringFromJNI() + ":" + testSwig.Area()

//        testSwig.testCallback(object : TestCallbackWrapper() {
//            override fun call(value: Double) {
//                Toast.makeText(applicationContext, "$value", Toast.LENGTH_LONG).show()
//            }
//        })


        val l = LegacyClass()
        l.set_property("Hello World!")
        Toast.makeText(applicationContext, "good:${l._property}", Toast.LENGTH_LONG).show()

//        TestJavaCpp.TestJavaCppClass().test(object :TestJavaCpp.TestJavaCppCallback(){
//            override fun call(value: Int): Boolean {
//                Toast.makeText(applicationContext, "good:${value}", Toast.LENGTH_LONG).show()
//                return true
//            }
//        })

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