package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.lang.IllegalStateException
import java.util.concurrent.Executors
import kotlin.concurrent.thread

fun main() {
    runBlocking {
        val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        val collectJob = Job()
        launch(dispatcher + collectJob) {
            getDataFlow()
                .catch {
                    log("catch : $it")
                }
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
                trySend(it)
                //close(IllegalStateException("error"))
                close()
                //offer(it)
            },
            {
                //offer("error")
                error("error")
            })
        awaitClose {
            cancel()
        }
    }
}


fun getData(success: ((str: String) -> Unit), error: (() -> Unit)) {
    thread {
        Thread.sleep(1000)
        success("1")
    }
}