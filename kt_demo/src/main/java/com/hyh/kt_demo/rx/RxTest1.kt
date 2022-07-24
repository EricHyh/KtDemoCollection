package com.hyh.kt_demo.rx

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx3.awaitFirst
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/9/24
 */
fun main() {

    println("start")

    runBlocking {
        Observable.create<Int> {
            it.onNext(1)
        }.awaitFirst()
    }


    Thread.sleep(1000000)
}