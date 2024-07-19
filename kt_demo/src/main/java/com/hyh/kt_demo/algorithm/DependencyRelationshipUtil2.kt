package com.hyh.kt_demo.algorithm


/**
 * 依赖关系工具类
 *
 * @author eriche 2024/4/9
 */
object DependencyRelationshipUtil2 {

    /**
     * 计算依赖顺序
     */
    fun <T> computeDependencyOrder(nodes: List<DependencyNode<T>>): Pair<List<T>, List<List<T>>> {
        val nodeValues = mutableListOf<T>()
        val dependencies: MutableList<Pair<T, T>> = mutableListOf()
        nodes.forEach { node ->
            nodeValues.add(node.value)
            node.dependents.forEach { dependent ->
                dependencies.add(Pair(node.value, dependent))
            }
        }
        return TopologicalSort<T>().findOrder(nodeValues, dependencies)
    }
}


fun main() {

    val pair1 = DependencyRelationshipUtil2.computeDependencyOrder(
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

    val pair2 = DependencyRelationshipUtil2.computeDependencyOrder(
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


    val pair3 = DependencyRelationshipUtil2.computeDependencyOrder(
        listOf(
            DependencyNode(0, listOf(1)),
            DependencyNode(1, listOf(0)),
            DependencyNode(2, listOf(3)),
            DependencyNode(3, listOf(4)),
            DependencyNode(4, listOf(0, 1, 2)),
        )
    )

    println(pair3)

    val pair4 = DependencyRelationshipUtil2.computeDependencyOrder(
        listOf(
            DependencyNode(0, listOf(1)),
            DependencyNode(1, listOf(0)),
            DependencyNode(2, listOf(3)),
            DependencyNode(3, listOf(4)),
            DependencyNode(4, listOf(0, 1, 2, 5)),
            DependencyNode(5, listOf(0, 1)),
        )
    )

    println(pair4)

}





