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

    val scope = CoroutineScope(SupervisorJob())

    val flow:Flow<Int> = flow {
        emit(0)
        delay(100)
        emit(1)
        delay(100)
        emit(2)
    }.shareIn(scope, SharingStarted.Lazily)

    runBlocking {

        val launch1 = launch {
            flow.collect {
                println("collect1 $it")
            }
        }

        delay(100)

        val launch2 = launch {
            flow.collectLatest {  }
            flow.collect {
                println("collect2 $it")
                delay(200)
            }
        }

        delay(1000)
        launch1.cancel()
        launch2.cancel()
    }
}


