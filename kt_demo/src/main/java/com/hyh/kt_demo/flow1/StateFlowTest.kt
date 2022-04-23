package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

fun main() {
    /*runBlocking {
        launch(Dispatchers.Default) {
            StateFlowTest1
                .state
                .collect {
                    log("collect:$it")
                }
        }
        log("launch")
        launch(Dispatchers.IO) {

            log("launch 1")

            val tickerChannel = ticker(
                delayMillis = 100,
                initialDelayMillis = 0, mode = TickerMode.FIXED_PERIOD
            )
            *//*tickerChannel.consumeEach {
                log("consumeEach")
            }*//*
            tickerChannel
                .consumeAsFlow()
                .collect {
                    log("tickerChannel collect")
                    StateFlowTest1.state.value = StateFlowTest1.state.value + 1
                }

            log("launch 2")
        }
    }*/


    runBlocking {


        launch(Dispatchers.IO) {
            log("launch2")
            StateFlowTest1
                .state
                .asSharedFlow()
                .onStart {
                    log("onStart")
                }
                .collect {
                    log("collect:$it")
                }
        }

        launch (Dispatchers.IO){
            log("launch1")
            for (num in 0..1000) {
                val tryEmit = StateFlowTest1.state.emit(num)
                log("for $num - tryEmit = $tryEmit")
            }
        }

    }






}

object StateFlowTest1 {

    val state: MutableSharedFlow <Int> = MutableSharedFlow ()

}