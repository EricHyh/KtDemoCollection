package com.hyh.kt_demo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEmpty
import kotlin.coroutines.Continuation
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime


data class TestData(
    val marketId: List<Int>
)


fun main(args: Array<String>) {


    val mutableListOf = mutableListOf<TestData>()

    for (index in 0..100) {
        mutableListOf.add(TestData(listOf(1, 2, 3, 4, 5)))
    }

    val time = measureTimeMillis {

        for (index in 0..100) {
            mutableListOf.filter {
                it.marketId.contains(1)
                        && it.marketId.contains(2)
                        && it.marketId.contains(3)
                        && it.marketId.contains(4)
                        && it.marketId.contains(5)
            }
        }
    }

    println("use time = $time")



    print("")


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

        GlobalScope.async {
        }

    }
}

suspend fun get(): Deferred<Int> {
    return CoroutineScope(currentCoroutineContext()).async {
        1
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