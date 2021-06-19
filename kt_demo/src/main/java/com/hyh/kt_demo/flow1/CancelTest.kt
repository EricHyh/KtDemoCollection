package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.EmptyCoroutineContext

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/6/19
 */

fun main() {

    val coroutineScope = CoroutineScope(EmptyCoroutineContext) + SupervisorJob()

    runBlocking {
        coroutineScope.launch {
            testCancel()
        }

        launch {
            delay(5000)
            coroutineScope.cancel()
            coroutineScope_testCancel.cancel()
            //println(coroutineScope_testCancel.isActive)
        }


        launch {
            repeat(100){
                delay(1000)

            }
        }


    }





}

val coroutineScope_testCancel = CoroutineScope(EmptyCoroutineContext) + SupervisorJob()
suspend fun testCancel() {
    val channel = Channel<Int>(Channel.BUFFERED)
    var num = 0

    coroutineScope_testCancel.launch {

        coroutineScope_testCancel.launch {
            channel.receiveAsFlow().collect {
                println("testCancel collect: $it")
            }
        }

        coroutineScope_testCancel.launch {
            repeat(100) {
                delay(1000)
                println("send:${num}")
                channel.send(num++)
            }
        }
    }


    coroutineScope_testCancel.coroutineContext[Job]!!.invokeOnCompletion {
        println("invokeOnCompletion")
    }
}