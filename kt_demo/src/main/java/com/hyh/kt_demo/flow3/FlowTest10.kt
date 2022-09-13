package com.hyh.kt_demo.flow3

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val linkedBlockingDeque = LinkedBlockingDeque<Int>()

    val thread = object : Thread() {

        override fun run() {
            super.run()
            var number = 0
            while (true) {
                if (isInterrupted) {
                    linkedBlockingDeque.offer(-1)
                    break
                }
                try {
                    sleep(500)
                } catch (e: Exception) {
                    linkedBlockingDeque.offer(-1)
                    break
                }
                linkedBlockingDeque.offer(number++)
            }
        }
    }
    thread.start()

    val flow = flow {
        while (true) {
            currentCoroutineContext().ensureActive()
            val take = linkedBlockingDeque.take()
            if (take == -1) {
                break
            }
            emit(take)
        }
    }

    runBlocking {
        val job = launch(Dispatchers.IO) {
            flow
                .collect {
                    println("collect: $it")
                }
        }


        delay(5000)
        println("interrupt")
        thread.interrupt()
        job.cancel()
    }
}