package com.hyh.kt_demo.flow3

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val dispatcher1 = createDispatcher("dispatcher1")
    val dispatcher2 = createDispatcher("dispatcher2")
    val dispatcher3 = createDispatcher("dispatcher3")

    val coroutineScope = CoroutineScope(dispatcher1)

    runBlocking {
        val flow = flow {
            println("flow collector: ${Thread.currentThread()}")
            emit(0)
        }


        val launch = coroutineScope.launch {
            flow
                .onEach {
                    println("onEach: ${Thread.currentThread()}")
                }
                .flowOn(dispatcher2)
                .onStart {
                    println("onStart: ${Thread.currentThread()}")
                }
                .map {
                    println("map: ${Thread.currentThread()}")
                    it
                }
                .flowOn(dispatcher3)
                .onEach {
                    println("onEach: ${Thread.currentThread()}")
                }
                .collect {
                    println("collect: ${Thread.currentThread()}")
                }
        }


        delay(5000)
        launch.cancel()





    }



    /*Observable
        .just(1)
        .map {
            println("map1: ${Thread.currentThread()}")
            it * 100
        }
        .subscribeOn(Schedulers.io()) //指定上游在 io 线程订阅
        .doOnSubscribe {
            println("doOnSubscribe:${Thread.currentThread()}")
        }
        .map {
            println("map2: ${Thread.currentThread()}")
            it * 100
        }
        .subscribeOn(Schedulers.computation())
        .subscribe {
            println("subscribe: ${Thread.currentThread()}")
        }




    Thread.sleep(1000)*/
}


private fun createDispatcher(name: String): CoroutineDispatcher {
    return Executors.newSingleThreadExecutor {
        Thread(it, name).apply {
            isDaemon = true
        }
    }.asCoroutineDispatcher()
}