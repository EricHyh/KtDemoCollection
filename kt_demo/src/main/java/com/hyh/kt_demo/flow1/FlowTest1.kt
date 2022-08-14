package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.Runnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.coroutines.EmptyCoroutineContext

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {


    val dispatcher1 =
        Executors.newCachedThreadPool { Thread("dispatcher1") }.asCoroutineDispatcher()
    val dispatcher2 =
        Executors.newCachedThreadPool { Thread("dispatcher2") }.asCoroutineDispatcher()


    val coroutineScope = CoroutineScope(Dispatchers.Main)
    runBlocking {
        val flow = flow<Int> {
            emit(0)
            emit(1)
        }
        coroutineScope.launch {
            flow
                .flowOn(dispatcher1)
                .flowOn(dispatcher2)
                .collect {
                    println(Thread.currentThread().name)
                }
        }
    }


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


    //val state = MutableStateFlow(Pair<Int, Boolean?>(0, null))

    //val flow1 = state.mapNotNull { it.second }


    /*val flow2 = simpleChannelFlow<Int> {
        println("simpleChannelFlow")
        flow1
            .onStart {
                emit(true)
            }
            .collect {
            send(100)
            println("flow1.collect: $it")
        }
    }*/

    runBlocking {


        val launch = launch(Dispatchers.IO) {
            testFlow()
        }

        launch {
            repeat(100) {
                delay(100)
                println("repeat：$it")
                state.value = Pair(state.value.first + 1, false)
            }
        }

        launch {
            delay(3000)
            coroutineScope.cancel()
            testFlow()
            /*launch {
                state.collect {
                    println("collect2：$it")
                }
            }*/
        }


        /*launch(Dispatchers.IO) {
            flow2.collect {
                println("flow2.collect: $it")
            }
        }*/

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

val state = MutableStateFlow(Pair<Int, Boolean?>(0, null))
val coroutineScope = CoroutineScope(EmptyCoroutineContext) + SupervisorJob()

suspend fun testFlow() {
    coroutineScope.launch {
        state.asStateFlow().collect {
            println("collect1：$it")
        }
    }
}

interface A {

    fun test(a: Int)

}


