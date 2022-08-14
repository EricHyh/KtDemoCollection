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

fun main() = runBlocking {

    val stateFLow = MutableStateFlow<Int>(0)

    launch {
        repeat(10) {
            delay(1000)
            stateFLow.value = ++stateFLow.value
            println("repeat ${stateFLow.value}")
        }

    }

    launch {
        stateFLow.collect {
            println("collect: $it")
        }
        println("stateFLow end")
    }

    val dispatcher1 =
        Executors.newCachedThreadPool { Thread(it, "dispatcher1") }.asCoroutineDispatcher()
    val dispatcher2 =
        Executors.newCachedThreadPool { Thread(it, "dispatcher2") }.asCoroutineDispatcher()
    val dispatcher3 =
        Executors.newCachedThreadPool { Thread(it, "dispatcher3") }.asCoroutineDispatcher()

    /*runBlocking {
        *//*println("coroutineScope1:$this")
        val flow = flow<Int> {
            println("emit in ${Thread.currentThread().name}")
            emit(0)
            delay(1000)
            emit(1)
        }
        launch {
            println("coroutineScope2:$this")
            delay(3000)
            flow.collect {
                println("collect1 start: $it")
                coroutineScope {
                    println("coroutineScope3:$this")
                    launch {
                        delay(1000)
                        println("delay $$it end")
                    }
                }
                println("collect1 end: $it")
            }
            println("collect1: end")
        }
        println("launch: end")*//*
        *//*launch {
            flow.collect {
                println("collect2: $it")
            }
            println("collect2: end")
        }*//*


        val stateFLow = MutableStateFlow<Int>(0)

        launch {
            repeat(10) {
                delay(1000)
                stateFLow.value = ++stateFLow.value
                println("repeat ${stateFLow.value}")
            }

        }

        launch {
            stateFLow.collect {
                println("collect: $it")
            }
            println("stateFLow end")
        }



    }*/
}


