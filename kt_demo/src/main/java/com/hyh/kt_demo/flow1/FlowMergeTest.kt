package com.hyh.kt_demo.flow1

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
        val flow1 = flowOf(1).onEach { delay(300) }.flowOn(Dispatchers.Test1)
        val flow2 = flowOf(2).onEach { delay(100) }.flowOn(Dispatchers.Test2)

        merge(flow1, flow2).collect {
            launch(Dispatchers.Test1) {
                log(it)
            }
        }

        combine(flow1, flow2) { array ->
            array
        }.collect {
            it.forEach { v ->
                log(v)
            }
        }
    }
}
/*
Test1-0 - 1610187897721 : onEach - 1
Test1-1 - 1610187897850 : onEach - 2
main - 1610187897852 : collect - 1
Test1-2 - 1610187897952 : onEach - 3
main - 1610187897952 : collect - 2
main - 1610187898054 : collect - 3
Test2-0 - 1610187898071 : onCompletion
*
* */