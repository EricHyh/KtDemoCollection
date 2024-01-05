package com.hyh.kt_demo.coroutine_demo1

import com.hyh.kt_demo.coroutine.SingleRunner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import java.io.Closeable
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * 调用栈测试
 *
 * @author eriche 2023/4/27
 */


fun main() {
    Dispatchers.setMain(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    runBlocking {
        launch {
            withContext(Dispatchers.Main) {
                println("$this first 1")
                for (i in 1..10) {
                    Test1().action(flowOf(i))
                }
                println("$this first 2")
            }
        }
        launch {
            println("1")
            withContext(Dispatchers.Main){
                println("2")
                for (i in 1..100){
                    println("for $i")
                }
            }
            println("3")
        }
    }
}

/**
 * 可关闭的协程作用域
 *
 * @author eriche
 * @data 2021/6/18
 */
internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {

    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}


class Test1{

    private val coroutineScope =
        CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val collectFromRunner = SingleRunner()
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    suspend fun action(flow: Flow<Int>) {
        coroutineScope.launch {
            collectFromRunner.runInIsolation {
                flow.collect {
                    withContext(mainDispatcher) {
                        println("$this action ${Thread.currentThread().stackTrace}")
                    }
                }
            }
        }
    }
}
