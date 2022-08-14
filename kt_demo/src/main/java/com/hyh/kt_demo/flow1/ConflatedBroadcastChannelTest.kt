package com.hyh.kt_demo.flow1

import com.hyh.kt_demo.coroutine.SimpleMutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/6/22
 */
fun main() {
    val channel = ConflatedBroadcastChannel(0)

    val simpleMutableStateFlow = SimpleMutableStateFlow(0)

    runBlocking {

        launch {
            /*channel.asFlow().collect {
                println("collect:$it")
            }*/
            simpleMutableStateFlow.flow.collect {
                println("collect:$it")
            }
        }

        var num = 0

        launch {
            repeat(100) {
                delay(100)
                /*if (!channel.isClosedForSend) {
                    channel.offer(++num)
                }*/
                simpleMutableStateFlow.value = num++
            }
        }

        launch {
            delay(1000)
            simpleMutableStateFlow.close()
        }

        launch {
            delay(4000)
            println(simpleMutableStateFlow.value)
        }
    }
}