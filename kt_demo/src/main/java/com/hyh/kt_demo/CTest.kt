package com.hyh.kt_demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

suspend fun <R, T> get(
    block: suspend R.() -> T,
    receiver: R,
    completion: Continuation<T>
): Int {
    block.startCoroutine(receiver, completion)
    return 0
}