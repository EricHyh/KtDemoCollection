package com.hyh.kt_demo.flow1

import jdk.nashorn.internal.objects.Global
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
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
            /*tickerChannel.consumeEach {
                log("consumeEach")
            }*/
            tickerChannel
                .consumeAsFlow()
                .collect {
                    //log("tickerChannel collect")
                    StateFlowTest1.state.value = StateFlowTest1.state.value + 1
                }

            log("launch 2")
        }
    }
}

object StateFlowTest1 {

    val state: MutableStateFlow<Int> = MutableStateFlow(0)

}