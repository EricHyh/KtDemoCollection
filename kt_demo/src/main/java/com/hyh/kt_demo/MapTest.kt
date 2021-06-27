package com.hyh.kt_demo


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
}


