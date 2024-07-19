package com.example.ndkdemo

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.ndk_demo_lib.TestJNI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.tv_content)
        textView.text = TestJNI().stringFromJNI()
    }
}