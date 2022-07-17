package com.hyh.kt_demo.flow1

import com.hyh.kt_demo.IEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
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
        /*println("runBlocking start")

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
            }
            println("lifecycleContext end")
        }


        launch {
            delay(5000)
            lifecycleContext.cancel()
            println("cancel")

        }
        //delay(10000)
        println("runBlocking end")*/




        val channelFlow = channelFlow<Int> {
            delay(100)
            repeat(10){
                send(it)
            }
        }


        launch {
            delay(500)
            lifecycleContext.cancel()
            println("cancel")

        }


        launch(lifecycleContext) {
            channelFlow.collectLatest {
                delay(100)
                println("channelFlow1:$it")
            }
        }


        launch {
            delay(5000)
            channelFlow.collectLatest {
                delay(100)
                println("channelFlow2:$it")
            }
        }

        println("end")


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



