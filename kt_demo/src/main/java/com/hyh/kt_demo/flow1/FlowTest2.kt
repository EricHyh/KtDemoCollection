package com.hyh.kt_demo.flow1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val treeSet = TreeSet<Int> { o1, o2 ->
        if (o1 == 0) return@TreeSet 0

        val i = o1 - o2
        if(i == 0) return@TreeSet 0
        return@TreeSet i
    }

    treeSet.addAll(listOf(1, 5, 3, 51, 3, 0, 1, 0))


    print("")

    runBlocking {
        val flow = flowOf(1)
        val flowOn = flow.flowOn(Dispatchers.IO)
        println("flow = $flow - ${flow.hashCode()}")
        println("flowOn = $flowOn - ${flowOn.hashCode()}")
        flow
            .flowOn(Dispatchers.IO)
            .collect() {
                println("ucollect1 : ${Thread.currentThread().name}")
                launch(Dispatchers.IO) {
                    println("collect2 : ${Thread.currentThread().name}")
                }
            }


    }
}