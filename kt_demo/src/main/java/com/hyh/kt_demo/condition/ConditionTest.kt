package com.hyh.kt_demo.condition

import java.util.*


interface IQueryCondition {

    val flag: Int

    fun getPriority(string: String): Int

}


class QueryCondition1(override val flag: Int = 1) : IQueryCondition {

    override fun getPriority(string: String): Int {
        return Math.max(string.length - 1, 0)
    }

}

class QueryCondition2(override val flag: Int = 1) : IQueryCondition {

    override fun getPriority(string: String): Int {
        return if (string.contains('a')) {
            1
        } else {
            0
        }
    }
}

class QueryCondition3(override val flag: Int = 1) : IQueryCondition {

    override fun getPriority(string: String): Int {
        return if (string.contains('b')) {
            1
        } else {
            0
        }
    }
}

class QueryCondition4(override val flag: Int = 1) : IQueryCondition {

    override fun getPriority(string: String): Int {
        return if (string.contains('c')) {
            1
        } else {
            0
        }
    }
}

class QueryCondition5(override val flag: Int = 1) : IQueryCondition {

    override fun getPriority(string: String): Int {
        return if (string.length > 5) {
            1
        } else {
            0
        }
    }
}

infix fun IQueryCondition.and(condition: IQueryCondition): IQueryCondition {
    return AndQueryCondition(this, condition)
}


infix fun IQueryCondition.or(condition: IQueryCondition): IQueryCondition {
    return OrQueryCondition(this, condition)
}


class OrQueryCondition(
    private val first: IQueryCondition,
    private val second: IQueryCondition
) : IQueryCondition {

    override val flag: Int = first.flag.coerceAtLeast(second.flag) + 1


    override fun getPriority(string: String): Int {
        val priority1 = first.getPriority(string)
        if (priority1 > 0) return priority1

        val priority2 = second.getPriority(string)
        if (priority2 > 0) return flag

        return 0
    }

}


class AndQueryCondition(
    private val first: IQueryCondition,
    private val second: IQueryCondition
) : IQueryCondition {

    override val flag: Int = first.flag.coerceAtLeast(second.flag)

    override fun getPriority(string: String): Int {
        val priority1 = first.getPriority(string)
        if (priority1 <= 0) return 0

        val priority2 = second.getPriority(string)
        if (priority2 <= 0) return 0

        return Math.min(priority1, priority2)
    }

}

fun IQueryCondition.query(): List<String> {

    val treeSet = TreeSet<String> { tradeAccount1, tradeAccount2 ->
        val priority1 = getPriority(tradeAccount1)
        if (priority1 == 0) return@TreeSet 0
        val priority2 = getPriority(tradeAccount2)
        val diff = priority1 - priority2
        if (diff == 0) return@TreeSet 1
        diff
    }

    //treeSet.addAll(listOf("1"))
    treeSet.addAll(
        listOf(
            "123456", "c", "fgf", "1",
            "12456c", "fgf", "ba", "ab", "c", "b", "12456b", "f", "aa", "bb", "12a456", "12456ba"
        )
    )

    treeSet.addAll(
        listOf(
            "bb"
        )
    )

    val list = treeSet.toMutableList()

    val first = list.firstOrNull()
    if (first != null && getPriority(first) == 0) {
        list.removeAt(0)
    }
    return list
}


fun main() {

    val iQueryCondition =
        QueryCondition2() or (QueryCondition3() or QueryCondition4() or QueryCondition5())


    val query1 =
        (QueryCondition2() or QueryCondition3() or QueryCondition4())
            .or(QueryCondition5())
            .query()

    val query2 =
        ((QueryCondition1() and (QueryCondition2() or QueryCondition3() or QueryCondition4())))
            .or(QueryCondition5())
            .query()

    val query3 =
        ((QueryCondition2() or QueryCondition3() or QueryCondition4()) and QueryCondition1())
            .or(QueryCondition5())
            .or(QueryCondition5())
            .query()

    print("")

}