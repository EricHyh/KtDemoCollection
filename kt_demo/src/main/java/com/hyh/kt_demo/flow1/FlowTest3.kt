package com.hyh.kt_demo.flow1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */


fun main() {
    runBlocking(Dispatchers.IO) {
        val time = measureTimeMillis {
            /*simple()
                .collectLatest { value -> // 取消并重新发射最后一个值
                    println("Collecting $value")
                    delay(300) // 假装我们花费 300 毫秒来处理它
                    println("Done $value")
                }*/

            simple()
                .conflate()
                .collect { value ->
                    println("Collecting $value")
                    delay(300) // 假装我们花费 300 毫秒来处理它
                    println("Done $value")
                }
        }
        println("Collected in $time ms")
    }
}

fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // 假装我们异步等待了 100 毫秒
        emit(i) // 发射下一个值
    }
    //200 1 200 2 200 3
    //    1 300   done
    //          2 300
}