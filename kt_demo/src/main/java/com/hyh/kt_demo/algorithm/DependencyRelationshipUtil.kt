package com.hyh.kt_demo.algorithm

import java.util.ArrayList


/**
 * 依赖关系工具类
 *
 * @author eriche 2024/4/9
 */
object DependencyRelationshipUtil {

    /**
     * 计算依赖顺序
     */
    fun <T> computeDependencyOrder(nodes: List<DependencyNode<T>>): Pair<List<T>, List<CircularDependency<T>>> {
        val dependencyNodes = DependencyNodes<T>()
        val allCircularDependencies: MutableList<CircularDependency<T>> = ArrayList()
        nodes.forEach {
            combine(dependencyNodes, it, allCircularDependencies)
        }
        return Pair((dependencyNodes.toList()), allCircularDependencies)
    }

    /**
     * 将[node]合并到[nodes]中
     */
    private fun <T> combine(
        nodes: DependencyNodes<T>,
        node: DependencyNode<T>,
        allCircularDependencies: MutableList<CircularDependency<T>>
    ) {
        if (nodes.unfixedContains(node.value)) {
            val circularDependencies = detectCircularDependencies(nodes, node)
            allCircularDependencies.addAll(circularDependencies)
            if (circularDependencies.isEmpty()) {
                nodes.addFixed(0, node.value)
            } else {
                nodes.addFixed(node.value)
            }
            nodes.removeUnfixed(node.value)
        } else {
            nodes.addFixed(node.value)
        }

        val dependentNodes = node.dependents
        dependentNodes.forEach {
            if (!nodes.contains(it)) {
                nodes.addUnfixed(it)
            }
        }
    }

    /**
     * 检测循环依赖
     */
    private fun <T> detectCircularDependencies(
        nodes: DependencyNodes<T>,
        node: DependencyNode<T>,
    ): List<CircularDependency<T>> {
        if (!nodes.unfixedContains(node.value)) {
            return ArrayList()
        }
        val dependentNodes = node.dependents
        val dependentNodesInFixed = dependentNodes.filter {
            nodes.fixedContains(it)
        }
        return dependentNodesInFixed.map {
            CircularDependency(node.value, it)
        }
    }


    /**
     * 依赖节点列表，存储结构如下：
     *
     * - |-------unfixed-------|    |---unfixed---|
     * - node1 -> node2 -> node3 -> node4 -> node5
     *
     */
    private class DependencyNodes<T> {

        /**
         * 未固定的节点
         */
        private val unfixed: MutableList<T> = ArrayList()

        /**
         * 以固定的节点
         */
        private val fixed: MutableList<T> = ArrayList()

        /**
         * 节点的相对位置
         */
        private val nodeRelativePosition: MutableMap<Pair<T, T>, Int> = mutableMapOf()

        operator fun contains(node: T): Boolean {
            return unfixed.contains(node) || fixed.contains(node)
        }

        fun unfixedContains(node: T): Boolean {
            return unfixed.contains(node)
        }

        fun fixedContains(node: T): Boolean {
            return fixed.contains(node)
        }

        fun addFixed(index: Int, node: T) {
            fixed.add(index, node)
        }

        fun addFixed(node: T) {
            var insertIndex = 0

            fixed.add(node)
        }

        fun removeFixed(node: T): Boolean {
            return fixed.remove(node)
        }

        fun addUnfixed(index: Int, node: T) {
            unfixed.add(index, node)
        }

        fun addUnfixed(node: T) {
            unfixed.add(node)
        }

        fun removeUnfixed(node: T): Boolean {
            return unfixed.remove(node)
        }

        fun toList(): List<T> {
            return unfixed + fixed
        }
    }
}

/**
 * 依赖节点
 */
data class DependencyNode<T> constructor(
    val value: T,
    val dependents: List<T>
)

/**
 * 循环依赖
 */
data class CircularDependency<T> constructor(
    val first: T,
    val second: T,
)

fun main() {
    val pair1 = DependencyRelationshipUtil.computeDependencyOrder(
        listOf(
            DependencyNode(0, listOf(1, 2, 3)),
            DependencyNode(1, listOf(4, 5)),
            DependencyNode(2, listOf(3, 4)),
            DependencyNode(3, listOf(4)),
            DependencyNode(4, listOf(5)),
            DependencyNode(5, listOf()),
        )
    )

    println(pair1)

    val pair2 = DependencyRelationshipUtil.computeDependencyOrder(
        listOf(
            DependencyNode(0, listOf(1, 2, 3)),
            DependencyNode(1, listOf(4, 5)),
            DependencyNode(2, listOf(3, 4)),
            DependencyNode(3, listOf(4, 1)),
            DependencyNode(4, listOf(5)),
            DependencyNode(5, listOf()),
        )
    )
    //123 0
    //45 1  -> 2345 10
    //34 2  -> 345 210

    //3 -> 210  -> 3210  -> 1320

    //41 3  -> 45 2103 -> 45 2130

    //5 4   -> 5 42130

    println(pair2)

}





