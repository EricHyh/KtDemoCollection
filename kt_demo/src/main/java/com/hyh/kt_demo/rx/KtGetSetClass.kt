package com.hyh.kt_demo.rx


fun main() {
    val testGetSet = TestGetSet()
    val testNum = testGetSet.testNum

    TestData.num = 5

    testGetSet.testNum = 4

    println("")


    Thread.sleep(200000)
}


class TestGetSet {

    var testNum: Int = 10
        get() = TestData.num
        set(value) {
            val oldValue = field
            println("set$oldValue, $value")
            if (oldValue == value) return
            TestData.num = value
        }
}


object TestData {

    var num = 0

}