package com.hyh.kt_demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main(args: Array<String>) {
    runBlocking {
        emptyFlow<String>()
            .onEmpty {
                emit(withContext(Dispatchers.IO) {
                    getData1()
                })

            }.combine(emptyFlow<Int>()
                .onEmpty {
                    emit(withContext(Dispatchers.IO) {
                        getData2()
                    })
                }) { a, b ->
                "$a -> $b"
            }.collect {
                println(it)
            }


    }
}


fun getData1(): String {
    Thread.sleep(1000)
    return "str"
}

fun getData2(): Int {
    Thread.sleep(2000)
    return 2
}