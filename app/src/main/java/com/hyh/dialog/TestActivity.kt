package com.hyh.dialog

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    var mFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    private fun test() {
        var num = 0
        GlobalScope.launch {
            num++
            val data = getData()
            val await = data.await()
            num++
            mFlag = true
            Log.d("TestActivity", "$await, $num, $mFlag")
        }
    }

    private suspend fun getData() = GlobalScope.async {
        "1"
    }

}