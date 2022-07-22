package com.hyh.kt_demo.flow2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

suspend fun main() {



    val simpleMutableStateFlow = SimpleMutableStateFlow<Int?>(null)

    runBlocking {


        launch {
            repeat(10){
                delay(100)
                simpleMutableStateFlow.value = it
            }
            simpleMutableStateFlow.value = null
        }



        launch {
            simpleMutableStateFlow.asStateFlow().collect {
                println("simpleMutableStateFlow:$it")
            }
        }




    }




}