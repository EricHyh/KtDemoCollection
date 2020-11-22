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

        invoke<Int, String> {
            Log.d("TestActivity", "onCreate: xxxxx")
            "2"
        }
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