package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {



    val dispatcher1 =
        Executors.newSingleThreadExecutor { Thread(it, "dispatcher1") }.asCoroutineDispatcher()
    val dispatcher2 =
        Executors.newSingleThreadExecutor { Thread(it, "dispatcher2") }.asCoroutineDispatcher()
    val dispatcher3 =
        Executors.newSingleThreadExecutor { Thread(it, "dispatcher3") }.asCoroutineDispatcher()

    val coroutineScope = CoroutineScope(dispatcher1)


    runBlocking {
        println("runBlocking ${this.coroutineContext} ${currentCoroutineContext()}")
        val flow = flow {
            println("emit in ${currentCoroutineContext()}")
            emit(0)
            emit(1)
        }
        coroutineScope.launch {
            println("launch ${this.coroutineContext} ${currentCoroutineContext()}")
            flow
                .flowOn(dispatcher2)
                .map {
                    println("map in ${currentCoroutineContext()}")
                    it
                }
                .flowOn(dispatcher3)
                .collect {
                    println("collect in ${coroutineScope.coroutineContext} ${currentCoroutineContext()}")
                }
        }
        /*coroutineScope.launch {
            println("launch2 ${this.coroutineContext} ${currentCoroutineContext()}")
        }*/
    }
}
