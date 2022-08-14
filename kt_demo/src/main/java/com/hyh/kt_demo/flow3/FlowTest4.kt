package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.internal.ChannelFlow
import java.util.concurrent.Executors

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val flow = channelFlow {
        send(0)
    }

    runBlocking {

    }

    println("xx")
}
