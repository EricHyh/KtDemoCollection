package com.hyh.kt_demo.flow2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

suspend fun main() {

    GlobalScope.launch {

    }

    flow<Int> {

    }.map {

    }.flowOn(Dispatchers.IO).collect {

    }

}