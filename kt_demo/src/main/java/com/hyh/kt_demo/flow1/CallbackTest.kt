package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.concurrent.Executors
import kotlin.concurrent.thread

fun main() {
    runBlocking {
        val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        val collectJob = Job()
        launch(dispatcher + collectJob) {
            getDataFlow()
                .collect {
                    log(it)
                }
        }


        launch {
            delay(3000)
            //collectJob.cancel()
        }
    }
}


@ExperimentalCoroutinesApi
fun getDataFlow(): Flow<String> {
    return callbackFlow {
        getData(
            {
                offer(it)
            },
            {
                offer("error")
            })
        awaitClose {
            log("awaitClose")
        }
    }
}


fun getData(success: ((str: String) -> Unit), error: (() -> Unit)) {
    thread {
        Thread.sleep(10000)
        success("1")
    }
}