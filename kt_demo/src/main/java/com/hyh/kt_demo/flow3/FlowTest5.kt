package com.hyh.kt_demo.flow3

import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val flow = flow {
        emit(0)
        emit(1)
    }.map { value ->
        println("map start: $value")
        val result = coroutineScope {
            withContext(Dispatchers.IO) {
                delay(100)
                value * 100
            }
        }
        println("map end: $value")
        result
    }

    runBlocking {
        flow.collect {
            println("collect $it")
        }
        println("collect end")
    }

    println("xx")
}