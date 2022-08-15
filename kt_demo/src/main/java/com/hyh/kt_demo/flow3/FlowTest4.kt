package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.internal.ChannelFlow
import java.util.concurrent.Executors

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val flow = flow {
        withContext(Dispatchers.IO){
            emit(0)
        }
        emit(1)
    }

    runBlocking {
        flow
            .collect {
                println("collect: $it")
            }
    }

    println("xx")
}


suspend fun getNumber(): Int {
    return withContext(Dispatchers.IO) {
        0
    }
}