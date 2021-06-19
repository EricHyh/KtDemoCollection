package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.EmptyCoroutineContext


fun main() {

    val coroutineScope = CoroutineScope(EmptyCoroutineContext) + SupervisorJob()

    runBlocking {

        coroutineScope.launch {
            println("launch test1")
            test()
            println("launch test2")
        }

        launch {
            println("launch delay1")
            delay(5000)
            println("launch delay2")
            coroutineScope.cancel()
            coroutineScope1.cancel()
        }

    }
    println("main end")
}

val coroutineScope1 = CoroutineScope(EmptyCoroutineContext) + SupervisorJob()
suspend fun test() {
    println("test start")
    coroutineScope1.launch {

        coroutineScope1.coroutineContext[Job]!!.invokeOnCompletion {
            println("invokeOnCompletion")
        }

        /*cancelableChannelFlow<Int>(coroutineScope.coroutineContext[Job]!!) {
            println("cancelableChannelFlow")
            awaitClose {
                println("awaitClose")
            }
        }.collect {

        }*/
    }

}