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
            delay(120)
            sharedFlow.collect {
                println("collect1: $it")
                delay(500)
            }
        }

        launch {
            sharedFlow
                .shareIn(this, SharingStarted.WhileSubscribed(), 0)
                .onStart {
                    delay(120)
                }
                .collect {
                    println("collect2: $it")
                    delay(150)
                }
        }

        delay(1100)
        job.cancel()
    }
}