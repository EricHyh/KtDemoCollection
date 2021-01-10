package com.hyh.kt_demo.flow1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main() {
    measureTimeMillis {

        /*runBlocking {
            getFlows1().flattenMerge()
                .collect {
                    log(it)
                }
        }*/

        runBlocking {
            //merge()
            val array: Array<Flow<String>> = getFlows2().toTypedArray()
            merge(*array).collect {
                log(it)
            }
        }
    }.let {
        log("time:$it")
    }
}

fun getFlows2(): List<Flow<String>> {
    return (1..10).map { v1 ->
        flow {
            //delay(1000)
            Thread.sleep(1000)
            (1..10).forEach { v2 ->
                emit("$v1 - $v2")
            }
        }.flowOn(Dispatchers.IO)
    }
}


fun getFlows1(): Flow<Flow<String>> {
    return flow {
        (1..10).forEach { v1 ->
            delay(100)
            emit(flow {
                (1..10).forEach { v2 ->
                    emit("$v1 - $v2")
                }
            }/*.flowOn(Dispatchers.Test1)*/)
        }
    }
}

