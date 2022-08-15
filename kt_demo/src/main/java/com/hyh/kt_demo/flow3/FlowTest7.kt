package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
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

    val flow = channelFlow {
        withContext(Dispatchers.IO) {
            delay(1000)
            trySend(0)
        }
        withContext(Dispatchers.IO) {
            trySend(1)
        }
    }

    runBlocking {
        flow
            .collect {
                println("collect: $it")
            }
    }
}