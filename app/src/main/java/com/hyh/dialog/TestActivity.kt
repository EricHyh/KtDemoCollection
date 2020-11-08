package com.hyh.dialog

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    private fun test() {
        GlobalScope.launch {
            val data = getData()
            val await = data.await()
            Log.d("TestActivity", await)
        }
    }

    private suspend fun getData() = GlobalScope.async {
        "1"
    }

}