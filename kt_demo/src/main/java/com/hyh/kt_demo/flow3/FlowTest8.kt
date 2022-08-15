package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {


    val sharedFlow = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )




    runBlocking {

        val job = launch {
            repeat(10) {
                sharedFlow.emit(it)
                println("emit $it")
                delay(100)
            }
        }

        launch {
            sharedFlow.collect {
                delay(500)
                println("collect: $it")
            }
        }

        /*launch {
            sharedFlow.collect {
                println("collect2 start: $it")
                delay(150)
                println("collect2 end: $it")
            }
        }*/

        delay(1100)
        job.cancel()
    }
}