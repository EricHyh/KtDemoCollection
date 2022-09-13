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


    val mutableStateFlow = MutableStateFlow(0)


    runBlocking {

        launch {
            mutableStateFlow.collect {
                println("$it")
            }
        }

        launch {
            repeat(10) {
                mutableStateFlow.value = ++mutableStateFlow.value
                delay(100)
            }
        }

    }
}