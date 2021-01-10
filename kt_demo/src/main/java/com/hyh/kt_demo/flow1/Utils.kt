package com.hyh.kt_demo.flow1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

fun log(message: String) =
    println("${Thread.currentThread().name} - ${System.currentTimeMillis()} : $message")

fun log(any: Any) =
    println("${Thread.currentThread().name} - ${System.currentTimeMillis()} : $any")

val Dispatchers.Test1: ExecutorCoroutineDispatcher
    get() = Utils.Dispatcher1

val Dispatchers.Test2: ExecutorCoroutineDispatcher
    get() = Utils.Dispatcher2

object Utils {

    private val num1: AtomicInteger = AtomicInteger(0)
    private val num2: AtomicInteger = AtomicInteger(0)

    val Dispatcher1 = Executors.newFixedThreadPool(4) {
        Thread(it, "Test1-${num1.getAndIncrement()}")
    }.asCoroutineDispatcher()

    val Dispatcher2 = Executors.newFixedThreadPool(4) {
        Thread(it, "Test2-${num2.getAndIncrement()}")
    }.asCoroutineDispatcher()

}