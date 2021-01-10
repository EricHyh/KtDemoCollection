package com.hyh.kt_demo.flow1

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking


fun main() {
    runBlocking {
        flow {
            log("flow block")
            (1..10).forEach {
                emit(it)
            }
        }.shareIn(this, SharingStarted.Lazily, 0)
    }
}