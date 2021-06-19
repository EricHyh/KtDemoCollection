package com.hyh.paging3demo.list

import androidx.lifecycle.MutableLiveData
import kotlin.math.abs
import kotlin.random.Random

object ListConfig {

    val lastTypesLiveData = MutableLiveData<List<String>>()
    val typesLiveData = MutableLiveData<List<String>>()


    //val aliveItems = MutableLiveData(0)

    var aliveItems = 0


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


    private val types = listOf("A", "B", "C", "D", "E")

    private var index: Int = 0

    private val typesMap1 = mapOf(
        Pair(0, listOf("G", "C", "E", "D", "F", "I", "H", "J")),
        Pair(1, listOf("B", "G", "H", "I", "F", "J", "A", "D", "E", "C")),
    )


    private val typesMap2 = mapOf(
        Pair(0, listOf("H", "C", "B", "D", "G", "F", "E", "A", "I")),
        //Pair(1, listOf("E", "D", "J", "I", "G", "F", "H")),
        Pair(1, listOf("D", "J", "I", "G", "F", "H", "E")),
        //Pair(1, listOf("H", "D", "J", "I", "G", "F", "E")),
    )

    fun randomTypes(): List<String> {
        val random = Random(System.currentTimeMillis())
        //val types = typesMap2[(index++ % 2)]
        //val types = types
        //val newTypes = types!!

        val types = typesMap[abs(random.nextInt() % 18)]
        val newTypes = types!!.map {
            Pair(it, random.nextInt())
        }.sortedBy {
            it.second
        }.map {
            it.first
        }

        lastTypesLiveData.postValue(typesLiveData.value)
        typesLiveData.postValue(newTypes)
        return newTypes
    }



}