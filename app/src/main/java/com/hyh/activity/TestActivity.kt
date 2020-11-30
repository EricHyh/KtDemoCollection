package com.hyh.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.hyh.demo.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    var mFlag: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout_test)

        invoke<Int, String> {
            Log.d("TestActivity", "onCreate: xxxxx")
            "2"
        }

        UrlIDConfigManager.test()
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