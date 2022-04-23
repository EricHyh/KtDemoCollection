package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */
fun main() {
    runBlocking {
        val flow1 = flowOf("one", "two", "three").onEach { delay(300) }
        val flow2 = flowOf(1, 2, 3).onEach { delay(200) }

        flow1.zip(flow2) { v1, v2 ->
            "$v1 - $v2"
        }
            .onCompletion {

        }.collect {
            log(it)
        }

        /*flow1.combine(flow2) { v1, v2 ->
            "$v1 - $v2"
        }.collect {
            println(it)
        }*/
    }
}
