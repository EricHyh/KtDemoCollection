package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val dispatcher1 =
        Executors.newSingleThreadExecutor { Thread(it, "dispatcher1") }.asCoroutineDispatcher()
    val dispatcher2 =
        Executors.newSingleThreadExecutor { Thread(it, "dispatcher2") }.asCoroutineDispatcher()
    val dispatcher3 =
        Executors.newSingleThreadExecutor { Thread(it, "dispatcher3") }.asCoroutineDispatcher()



    val stateFlow = MutableStateFlow(0)

    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("")
    }

    val coroutineScope = CoroutineScope(dispatcher1) /*+ exceptionHandler*/

    runBlocking {

        val launch1 = launch {
            //throw CancellationException("测试一下")
            throw NullPointerException("测试一下")
            /*try {
                stateFlow
                    .onCompletion {
                        println("onCompletion: $this")
                    }
                    .collect {
                        println("collect: $it")
                    }
            } catch (e: Exception) {
                throw e
            }*/
        }
        /*launch {
            delay(2000)
            //launch1.cancel()
        }*/

    }

    println("xx")
}

suspend fun test1() {
    delay(1000)
    println("test1: ${currentCoroutineContext()}")
}

suspend fun test2() {
    delay(1000)
    println("test2: ${currentCoroutineContext()}")
}

