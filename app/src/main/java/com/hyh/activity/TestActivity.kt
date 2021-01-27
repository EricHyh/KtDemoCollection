package com.hyh.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hyh.demo.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.android.synthetic.main.auto_size_text_layout.*

class TestActivity : AppCompatActivity() {

    var mFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auto_size_text_layout)
    }

    fun setText1(view: View) {
        text_view.text = "1-2-3-4-5-6-7-8-9-10"
    }

    fun setText2(view: View) {
        text_view.text = "1-2-3-4-5-6-7-8-9-10-11-12-13-14-15"
    }

    fun setText3(view: View) {
        text_view.text = "1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20"
    }

    fun setText4(view: View) {
        text_view.text = "1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22-23-24-25-26-27-28-29-30"
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


    public operator fun <R, T> invoke(block: R.() -> T): Unit {
        Log.d("TestActivity", "invoke1: $this")
        block.xxx {

            Log.d("TestActivity", "invoke2: $this")
            it.printStackTrace()
        }
    }

}

internal fun <R, T> ((R) -> T).xxx(
    onCancellation: ((cause: Throwable) -> Unit)? = null
) {
    runSafely {

    }
    Log.d("TestActivity", "xxx: $this")
    onCancellation?.invoke(Exception("xxxx"))
}


private inline fun runSafely(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
    }
}

object UrlIDConfigManager {

    private const val TAG = "UrlIDConfigManager"

    var a = 0

    init {
        Log.d(TAG, "UrlIDConfigManager init")
        a = 1
    }

    public fun test() {
        Log.d(TAG, "UrlIDConfigManager test $a")
    }
}