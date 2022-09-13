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

    runBlocking {

        val launch = launch {
            getFlow(this)
                .collect {
                    println("collect1 $it")
                }
            flowOf(3, 4, 5)
                .collect {
                    println("collect2 $it")
                }
        }

        delay(1000)
        launch.cancel()
    }
}


private suspend fun getFlow(scope: CoroutineScope): Flow<Int> {
    return flow {
        emit(0)
        delay(100)
        emit(1)
        delay(100)
        emit(2)
    }.stateIn(scope)
}


