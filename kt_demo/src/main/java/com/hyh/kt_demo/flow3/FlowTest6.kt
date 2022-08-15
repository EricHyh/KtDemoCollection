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

    val flow = flow {
        coroutineScope {
            val channel = produce {
                withContext(Dispatchers.IO) {
                    send(1)
                }
                withContext(Dispatchers.Default) {
                    send(2)
                }
            }
            channel.consumeEach { value ->
                emit(value)
            }
        }
    }

    runBlocking {
        flow
            .collect {
                println("collect: $it")
            }
    }
}