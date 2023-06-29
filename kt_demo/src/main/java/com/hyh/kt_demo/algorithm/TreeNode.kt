package com.hyh.kt_demo.algorithm


/**
 * 节点
 *
 * @author eriche 2023/6/2
 */
class TreeNode(val value: Int) {

    var left: TreeNode? = null
    var right: TreeNode? = null

    companion object {

        //[1,2,null,4,5,6,null,8,9]
        //
        //            1
        //          /    \
        //         2     null
        //       /    \
        //      4      5
        //    /  \    /  \
        //   6  null  8   9
        fun create(vararg values: Int?): TreeNode? {
            if (values.isEmpty()) return null
            val list = values.toMutableList()
            val headerValue = list.removeAt(0) ?: return null
            return create(headerValue, list)
        }

        fun create(headerValue: Int, values: List<Int?>): TreeNode {
            val header = TreeNode(headerValue)
            val nodeList: MutableList<TreeNode> = mutableListOf()
            nodeList.add(header)
            var cursor = 0
            while (nodeList.isNotEmpty()) {
                val first = nodeList.removeAt(0)
                val newCursor = fillLeftAndRight(first, cursor, values)
                if (newCursor - cursor != 2) break
                cursor = newCursor
                if (first.left != null) {
                    nodeList.add(first.left!!)
                }
                if (first.right != null) {
                    nodeList.add(first.right!!)
                }
            }
            return header
        }


        private fun fillLeftAndRight(node: TreeNode, cursor: Int, values: List<Int?>): Int {
            var newCursor = cursor
            if (fillLeft(node, newCursor, values)) {
                newCursor++
            }
            if (fillRight(node, newCursor, values)) {
                newCursor++
            }
            return newCursor
        }

        private fun fillLeft(node: TreeNode, cursor: Int, values: List<Int?>): Boolean {
            if (cursor >= values.size) return false
            val value = values[cursor]
            if (value != null) {
                node.left = TreeNode(value)
            }
            return true
        }

        private fun fillRight(node: TreeNode, cursor: Int, values: List<Int?>): Boolean {
            if (cursor >= values.size) return false
            val value = values[cursor]
            if (value != null) {
                node.right = TreeNode(value)
            }
            return true
        }
    }
}