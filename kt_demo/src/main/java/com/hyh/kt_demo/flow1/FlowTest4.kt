package com.hyh.kt_demo.flow1

import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */


fun main() {
    runBlocking {
        /*val time = measureTimeMillis {
            val nums = (1..3).asFlow() // 数字 1..3
            val strs = flowOf("one", "two", "three") // 字符串
            nums.zip(strs) { a, b -> "$a -> $b" } // 组合单个字符串
                .collect { println(it) } // 收集并打印
        }
        println("Collected in $time ms")*/

        /*val startTime = System.currentTimeMillis() // 记录开始时间
        (1..3).asFlow().onEach { delay(100) } // 每 100 毫秒发射一个数字
            .flatMapLatest { requestFlow(it) }
            .collect { value -> // 收集并打印
                println("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }*/
        /*val startTime = System.currentTimeMillis() // 记录开始时间
        val time = measureTimeMillis {
            flowOf(1)
                .flatMapConcat {
                    (1..10).asFlow()
                }
                .flatMapConcat { v1 ->
                    flow<String> {
                        flowOf(v1)
                            .map { v2 ->
                                println("map: $v2 ,  ${Thread.currentThread().name} - ${System.currentTimeMillis()}")
                                delay(100)
                                "value->$v2"
                            }
                            .flowOn(Dispatchers.IO)
                            .collect {
                                emit(it)
                            }
                    }
                }
                .collect {
                    println("collect: $it ,  ${Thread.currentThread()}")
                }
        }*/

        val time = measureTimeMillis {
            flowOf(1)
                .buffer()
                .flatMapMerge {
                    combine(getFlow()) {
                        it
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { array ->
                    array.forEach { str ->
                        println("collect: value=$str , ${Thread.currentThread().name}")
                    }
                }
        }

        println("Collected in $time ms , ${Thread.currentThread().name}")
    }
}


fun getObservable(): List<Observable<String>> {
    val list = mutableListOf<Observable<String>>()
    for (index in 1..100) {
        list.add(Observable.just(index).map { "$it" })
    }
    return list
}

fun getFlow(): List<Flow<String>> {
    val list = mutableListOf<Flow<String>>()
    for (index in 1..10) {
        list.add(flow<String> {

            emit("value=$index")
            delay(200)
        })
    }
    return list
}


fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First")
    delay(500) // 等待 500 毫秒
    emit("$i: Second")
}