package com.hyh.kt_demo.flow1

import com.hyh.kt_demo.coroutine.SingleRunner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow

fun main() {


}


suspend fun test(singleRunner: SingleRunner, channel: Channel<Int>) {
    singleRunner.runInIsolation {
        channel
            .consumeAsFlow()


    }
}