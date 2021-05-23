package com.hyh.kt_demo.flow1

import com.sun.org.apache.xpath.internal.functions.Function2Args
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.Continuation

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {
    //collect(action) -> collect(FlowCollector) -> FlowCollector.block() ->
    //FlowCollector.emit(value) -> action(value)

    /*val a: A = object : A {
        override fun test(a: Int) {
            println("test:$a")
        }
    }

    val testFun = A::test as Function2<A, Int, Unit>
    testFun(a, 10)*/
    //val channel = Channel<Int>(capacity = Channel.RENDEZVOUS)


    val state = MutableStateFlow(Pair<Int, Boolean?>(Integer.MIN_VALUE, null))

    val flow1 = state.mapNotNull { it.second }

    val flow2 = simpleChannelFlow<Int> {
        println("simpleChannelFlow")
        flow1
            .onStart {
                emit(true)
            }
            .collect {
            send(100)
            println("flow1.collect: $it")
        }
    }



    runBlocking {


        launch(Dispatchers.IO) {
            flow2.collect {
                println("flow2.collect: $it")
            }
        }


        /*state.value = Pair(1, true)

        delay(100)

        state.value = Pair(1, false)*/


        /*launch(Dispatchers.IO) {
            channel
                .receiveAsFlow()
                .scan(0) { v1, v2 ->
                    println("scan:$v1 - $v2")
                    v1 + v2
                }
                .buffer(Channel.BUFFERED)
                .collect {
                    println("collect:$it")
                }
        }


        println("send")

        channel.send(100)
        channel.send(200)*/
    }


    /*runBlocking {
        flowOf(1)
            .scan(0) { i: Int, i1: Int ->
                println("scan:$i")
                i + i1
            }.collect {
                println("collect:$it")
            }


        *//*flowOf(1)
            .scan(0) { i: Int, i1: Int ->
                i + i1
            }.collect {
                println("collect:$it")
            }*//*
    }*/


}

interface A {

    fun test(a: Int)

}


