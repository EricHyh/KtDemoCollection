package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {


    runBlocking {

        /*launch {
            flowOf(1)
                .onStart {
                    println("发射数据前执行")
                }
                .onCompletion {
                    println("所有数据发射完成后执行")
                }
                .onEmpty {
                    println("上游一个数据都不发射时执行")
                }
                .onEach {

                }
                .catch {
                    println("异常时")
                }
                .collect {
                    println("$it")
                }
        }*/


        launch {
            flowOf(0, 1, 2)
                .collect {
                    println("collect $it")
                }
        }

    }
}