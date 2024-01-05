package com.hyh.kt_demo.coroutine_demo2

import kotlinx.coroutines.*
import kotlinx.coroutines.test.setMain
import java.util.concurrent.Executors

/**
 * 调用栈测试
 *
 * @author eriche 2023/4/27
 */


fun main() {
    Dispatchers.setMain(Executors.newSingleThreadExecutor {
        Thread(it, "setMain")
    }.asCoroutineDispatcher())

    val fixed = Executors.newFixedThreadPool(3) {
        Thread(it, "Fixed")
    }.asCoroutineDispatcher()


    val coroutineScope = CoroutineScope(Dispatchers.Main)


    runBlocking {

        coroutineScope.launch {

            println("$this ${Thread.currentThread().name} launch1")

            val withContext = withContext(fixed) {
                println("$this ${Thread.currentThread().name} withContext")
                delay(100)
                1
            }

            println("$this ${Thread.currentThread().name} launch2 $withContext")

        }

    }
}