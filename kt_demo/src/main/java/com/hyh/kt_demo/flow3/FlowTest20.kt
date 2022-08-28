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

    val stateFlow = MutableStateFlow(100)

    runBlocking {
        val launch = launch {
            stateFlow.collect {
                println("collect1: $it")
                delay(400)
            }
        }

        launch {
            repeat(10) {
                stateFlow.value = it
                delay(100)
            }
        }

        delay(2000)
        launch.cancel()
    }
}