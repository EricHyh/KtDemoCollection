package com.hyh.kt_demo.flow1

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * TODO: Add Description
 *
 * @author eriche 2022/5/30
 */


fun <T, R> Flow<T>.testMap1(transform: (value: T) -> R): Flow<R> {
    val originFlow = this
    return flow {
        originFlow.collect {
            emit(transform(it))
        }
    }
}


fun <T, R> Flow<T>.testMap2(transform: suspend (value: T) -> R): Flow<R> {
    val originFlow = this
    return flow {
        originFlow.collect {
            emit(transform(it))
        }
    }
}


fun <T, R> Flow<T>.testFlatMap(transform: suspend (value: T) -> Flow<R>): Flow<R> {
    val originFlow = this
    return flow {
        originFlow.collect {
            val transformFlow = transform(it)
            emitAll(transformFlow)
        }
    }
}


@InternalCoroutinesApi
fun <T> testMerge(flows: List<Flow<T>>): Flow<T> {
    /*return flow {
        val collector: FlowCollector<T> = this
        flows.forEach { flow ->
            flow.collect {
                collector.emit(it)
            }
        }
    }*/

    return channelFlow {
        flows.forEach { flow ->
            launch {
                flow.collect { send(it) }
            }
        }
    }
}


fun main() = runBlocking {
    flow {
        emit(1)
    }.testMap2 {
        "value = $it"
    }.collect {
        println("testMap2:$it")
    }


    flow {
        emit(1)
        emit(2)
    }.flatMapConcat {
        flow {
            emit("$it - 0")
            emit("$it - 1")
        }
    }.collect {
        println("testFlatMap:$it")
    }

}