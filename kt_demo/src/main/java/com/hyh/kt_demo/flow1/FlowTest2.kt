package com.hyh.kt_demo.flow1

import com.sun.org.apache.xpath.internal.functions.Function2Args
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {
    runBlocking {
        val flow = flowOf(1)
        val flowOn = flow.flowOn(Dispatchers.IO)
        println("flow = $flow - ${flow.hashCode()}")
        println("flowOn = $flowOn - ${flowOn.hashCode()}")
        flow
            .flowOn(Dispatchers.IO)
            .collect() {
                println("ucollect1 : ${Thread.currentThread().name}")
                launch(Dispatchers.IO) {
                    println("collect2 : ${Thread.currentThread().name}")
                }
            }


    }
}