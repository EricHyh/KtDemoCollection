package com.hyh.kt_demo.algorithm

/**
 * 节点
 *
 * @author eriche 2023/6/2
 */
class ListNode(val value: Int) {

    var next: ListNode? = null

    companion object {
        fun create(headerValue: Int, vararg values: Int): ListNode {
            val header = ListNode(headerValue)
            var cur = header
            values.forEach {
                val listNode = ListNode(it)
                cur.next = listNode
                cur = listNode
            }
            return header
        }
    }
}