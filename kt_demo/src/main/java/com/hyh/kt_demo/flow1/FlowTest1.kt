package com.hyh.kt_demo.flow1

import com.sun.org.apache.xpath.internal.functions.Function2Args
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
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

    val a: A = object : A {
        override fun test(a: Int) {
            println("test:$a")
        }
    }

    val testFun = A::test as Function2<A, Int, Unit>
    testFun(a, 10)

}

interface A {

    fun test(a: Int)

}
