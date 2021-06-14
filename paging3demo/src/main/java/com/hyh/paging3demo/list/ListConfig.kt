package com.hyh.paging3demo.list

import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

object ListConfig {

    val typesLiveData = MutableLiveData<List<String>>()

    fun randomTypes(): List<String> {
        val types = listOf("A", "B", "C", /*"D", "E", "F", "G", "H", "I", "J"*/)
        val random = Random(System.currentTimeMillis())
        val newTypes = types.map {
            Pair(it, random.nextInt())
        }.sortedBy {
            it.second
        }.map {
            it.first
        }
        typesLiveData.postValue(newTypes)
        return newTypes
    }


}