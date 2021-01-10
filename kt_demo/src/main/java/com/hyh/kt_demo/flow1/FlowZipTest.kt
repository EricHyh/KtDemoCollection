package com.hyh.kt_demo.flow1

import com.sun.org.apache.xpath.internal.functions.Function2Args
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.timer
import kotlin.coroutines.Continuation
import kotlin.system.measureTimeMillis

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
