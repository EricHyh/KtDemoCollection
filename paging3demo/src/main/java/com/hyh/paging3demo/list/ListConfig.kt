package com.hyh.paging3demo.list

import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

object ListConfig {

    val typesLiveData = MutableLiveData<List<String>>()

    private val typesMap = mapOf(
        Pair(0, listOf("A")),
        Pair(1, listOf("A", "B")),
        Pair(2, listOf("A", "B", "C")),
        Pair(3, listOf("A", "B", "C", "D")),
        Pair(4, listOf("A", "B", "C", "D", "E")),
        Pair(5, listOf("A", "B", "C", "D", "E", "F")),
        Pair(6, listOf("A", "B", "C", "D", "E", "F", "G")),
        Pair(7, listOf("A", "B", "C", "D", "E", "F", "G", "H")),
        Pair(8, listOf("A", "B", "C", "D", "E", "F", "G", "H", "I")),
        Pair(9, listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")),
        Pair(10, listOf("B", "C", "D", "E", "F", "G", "H", "I", "J")),
        Pair(11, listOf("C", "D", "E", "F", "G", "H", "I", "J")),
        Pair(12, listOf("D", "E", "F", "G", "H", "I", "J")),
        Pair(13, listOf("E", "F", "G", "H", "I", "J")),
        Pair(14, listOf("F", "G", "H", "I", "J")),
        Pair(15, listOf("G", "H", "I", "J")),
        Pair(16, listOf("H", "I", "J")),
        Pair(17, listOf("I", "J")),
        Pair(18, listOf("J")),
    )


    fun randomTypes(): List<String> {
        val random = Random(System.currentTimeMillis())
        val types = typesMap[Math.abs(random.nextInt() % 18)]
        val newTypes = types!!.map {
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