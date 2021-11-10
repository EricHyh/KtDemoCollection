package com.hyh.kt_demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.concurrent.thread

fun main() {

    val x: BigDecimal

    x.rem()




    thread {

        runBlocking {
            launch {
                repeat(100) {
                    println("index = $it")
                    delay(400)
                }
            }


            val nums = (1..3).asFlow().onEach { delay(300) } // 发射数字 1..3，间隔 300 毫秒
            val strs = flowOf("one", "two", "three").onEach { delay(400) } // 每 400 毫秒发射一次字符串
            val startTime = System.currentTimeMillis() // 记录开始的时间

            //nums.combineTransform()

            nums.combine(strs) { a, b -> "$a -> $b" } // 使用“combine”组合单个字符串
                .collect { value ->
                    // 收集并打印
                    println("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }



            flow<String> {

            }.collect(){

            }






            val xx: Flow<Int> = flowOf(1)

            xx.collect(){

            }

            fun getNum(): Int {
                return 1
            }

            ::getNum.asFlow()
                .collect() { a ->

                }



            flowOf("")
                .filter {
                    true
                }.map {
                    1
                }.flatMapConcat {
                    flowOf(1)
                }.onEmpty {
                    2
                }.onCompletion {

                }.onEach {

                }.collect {

                }
        }
    }
    Thread.sleep(200000)
}