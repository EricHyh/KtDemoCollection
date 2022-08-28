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
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    runBlocking {

        val job = launch {
            repeat(10) {
                sharedFlow.emit(it)
                println("emit $it")
                delay(100)
            }
        }

        val launch1 = launch {
            sharedFlow.collect {
                println("collect1: $it")
                delay(100)
            }
        }

        val launch2 = launch {
            sharedFlow.collect {
                println("collect2: $it")
                delay(800)
            }
        }

        delay(11000)
        launch1.cancel()
        launch2.cancel()
    }
}