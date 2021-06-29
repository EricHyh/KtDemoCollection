package com.hyh.kt_demo

import kotlin.math.sign


fun main() {
    val map = LinkedHashMap<Int, String?>()
    map.put(0, null)
    map.put(1, null)
    map.put(2, null)
    map.put(3, null)
    map.put(4, null)
    map.put(5, null)
    map.put(6, null)

    map.asIterable().forEach {
        println("${it.key} - ${it.value}")
    }

    println("-----------------------------------")

    map.put(0, "0")
    map.put(2, "2")
    map.put(5, "5")
    map.put(6, "6")

    map.asIterable().forEach {
        println("${it.key} - ${it.value}")
    }



    println("-----------------------------------")

    map.remove(5)

    map.asIterable().forEach {
        println("${it.key} - ${it.value}")
    }


    /*val list1 = listOf(Pair(0, "0"), Pair(1, "11"), Pair(2, "2"), Pair(3, "3"), Pair(4, "4"),  Pair(2, "222"),Pair(5, "2"))
    val list2 = listOf(Pair(0, "00"), Pair(1, "11"), Pair(2, "22"), Pair(3, "33"), Pair(4, "44"), Pair(5, "55"))*/

    val list1 = listOf(Pair(0, "0"), Pair(0, "0"))
    val list2 = listOf(Pair(0, "00"), Pair(0, "00"), Pair(1, "11"))

    val list2Map = mutableMapOf<Int, Boolean>()
    val resultList = mutableListOf<Pair<Int, String>>()

    resultList.addAll(list1)
    list2.forEach {
        list2Map[System.identityHashCode(it)] = true
        resultList.add(it)
    }

    resultList.sortWith(Comparator { o1, o2 ->
        val num1 = o1.first
        val num2 = o2.first
        val diff = num1 - num2
        if (diff == 0) {
            val isFundPosition1 = list2Map[System.identityHashCode(o1)] ?: false
            val isFundPosition2 = list2Map[System.identityHashCode(o2)] ?: false
            if (isFundPosition1 == isFundPosition2) {
                0
            } else {
                // 股票与基金盈亏比例相同时，股票在前，基金在后
                if (isFundPosition1) {
                    1
                } else if (isFundPosition2) {
                    -1
                } else {
                    0
                }
            }
        } else {
            -diff.sign
        }
    })

    val toDouble = "-0.0".toDouble()

    println()


}


