package com.hyh.kt_demo.flow1

import com.hyh.kt_demo.IEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

fun main() {

    val dispatcher = Executors.newSingleThreadExecutor {
        Thread(it, "test-main")
    }

    val channel = BroadcastChannel<Int>(100)

    //val eventFlow = MutableSharedFlow<Int>()



    var num = 0;

    //val tickerChannel = ticker(delayMillis = 1000, initialDelayMillis = 1000)

    val lifecycleContext: CoroutineContext = dispatcher.asCoroutineDispatcher() + Job()

    runBlocking {
        println("runBlocking start")

        println("receiveAsFlow1")

        launch {
            channel.openSubscription().consumeAsFlow()
                .collect {
                    println("receiveAsFlow1:${it}")
                }
        }


        println("receiveAsFlow2")

        launch {
            channel.openSubscription().consumeAsFlow()
                .collect {
                    println("receiveAsFlow2:${it}")
                }
        }

        launch(lifecycleContext) {
            println("lifecycleContext start")

            repeat(10) {
                delay(1000)
                println("send:${num}")
                channel.send(num++)
                channel.sendBlocking()
            }
            println("lifecycleContext end")
        }


        launch {
            delay(5000)
            lifecycleContext.cancel()
            println("cancel")

        }
        //delay(10000)
        println("runBlocking end")

    }


    /*runBlocking {
        println("runBlocking2 start")
        launch {
            channel.receiveAsFlow()
                .collect {
                    println("consumeAsFlow2:${it}")
                }
        }

        repeat(10) {
            delay(1000)
            println("send2:${num++}")
            channel.send(num++)
            //eventFlow.emit(num++)
        }
    }*/

    /*runBlocking {
        test(lifecycleContext)


        launch {
            delay(10000)
            lifecycleContext.cancel()
        }


    }*/







    print("end")
}

suspend fun test(lifecycleContext: CoroutineContext) {
    GlobalScope.launch(lifecycleContext) {
        repeat(100) {
            delay(1000)
            println("repeat:${it}")
        }
    }
}



