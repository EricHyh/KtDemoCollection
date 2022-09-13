package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {


    val flow = flow {
        var number = 0
        while (currentCoroutineContext().isActive) {
            emit(number++)
            delay(200)
        }
    }

    val coroutineScope = CoroutineScope(Dispatchers.IO)

    val sharedFlow = flow
        .shareIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(500, 1000),
            replay = 2
        )

    runBlocking {

        val launch1 = launch {
            sharedFlow
                .collect {
                    println("collect1: $it")
                }
        }
        delay(2000)
        launch1.cancel()
        println("launch1 end")

        delay(1000)

        val launch2 = launch {
            sharedFlow
                .collect {
                    println("collect2: $it")
                }
        }
        delay(2000)
        launch2.cancel()
        println("launch2 end")
    }
}