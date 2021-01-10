package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors


fun main() {
    runBlocking {
        val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        val collectJob = Job()
        val launch = launch(dispatcher /*+ collectJob*/) {
            val flow = (1..10).asFlow().onEach {
                delay(1000)
                //if (it == 5) throw NullPointerException()
            }.catch { e ->
                log("catch : $e")
            }.onCompletion { e ->
                log("onCompletion : $e")
            }
            flow.collect {
                log(it)
            }
        }

        launch {
            delay(3000)
            val job = dispatcher[Job]
            log("cancel $job - $launch")
            //launch.cancel(CancellationException("cancel"))
            collectJob.cancel(/*CancellationException("cancel")*/)
            log("cancel")
        }
    }
}