package com.hyh.kt_demo.algorithm

import kotlin.math.abs


fun main() {

    val listNode1 = ListNode.create(6, 2, 3, 8, 9, 11, 1, 2)
    val listNode2 = ListNode.create(6, 2, 3, 8, 9, 11, 1, 2)

    println(findMax(listNode1, 2))
    println(findMax(listNode1, 1))
    println(findMax(listNode1, 0))
    println(findMax(listNode1, 7))
}


fun findMax(node: ListNode, n: Int): Int {

    var first: ListNode = node
    var second: ListNode? = null
    var index = 0
    var max = 0

    while (true) {
        if (second != null) {
            max = Math.max(abs(first.value - second.value), max)
        }

        val nextNode = first.next ?: break

        first = nextNode
        second = second?.next

        if (index == n && second == null) {
            second = node
        }
        index++

    }

    return max
}