package com.hyh.kt_demo

import java.util.*


/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/29
 */
fun main() {


    val ENTRY_SPLIT = "@|@"
    val KEY_VALUE_SPLIT = "@=@"

    val entriesString1 = "100&abc${KEY_VALUE_SPLIT}true${ENTRY_SPLIT}101&abc${KEY_VALUE_SPLIT}false${ENTRY_SPLIT}102&abc${KEY_VALUE_SPLIT}false"

    val keyString = "102&abc"
    val valueString = "true"
    val newEntry = keyString + KEY_VALUE_SPLIT + valueString

    val index = entriesString1.indexOf("$keyString$KEY_VALUE_SPLIT")

    val nextEntrySplitIndex = entriesString1.indexOf(ENTRY_SPLIT, index + "$keyString$KEY_VALUE_SPLIT".length)

    val newEntriesString = if (nextEntrySplitIndex < 0) {
        entriesString1.substring(0, index) + newEntry
    } else {
        val oldEntry = entriesString1.substring(index, nextEntrySplitIndex)

        entriesString1.replace(oldEntry, newEntry)
    }


    //val regex = Regex("($keyString$KEY_VALUE_SPLIT)^(?!$ENTRY_SPLIT).+($ENTRY_SPLIT)")
    //val replace = entriesString1.replace(regex, "${newEntry}${ENTRY_SPLIT}")


    /*Integer[] array =
    {0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 2, 1, 0, 0, 0, 2, 30, 0, 3};*/


    /*val list1 = listOf(
        0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 2, 1, 0, 0, 0, 2, 30, 0, 3
    )*/


    /*val list2 = listOf<Int>(0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 2, 1, 0, 0, 0, 2, 30, 0, 3)
    val sortedWith = list2.sortedWith(Comparator { p0, p1 ->
        var i = p0 - p1
        if (i == 0) i = -1
        return@Comparator i
    })*/

    val list2 = listOf<Int>(0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0,  0, 0,
        0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 3)
    /*val sortedWith = list2.sortedWith(Comparator { p0, p1 ->
        var i = p0 - p1
        if (i == 0) i = -1
        return@Comparator i
    })*/



    /*val list2 = listOf<Int>(1, 1, 1, 0, 0, 1, 1, 0, 0)
    val sortedWith = list2.sortedWith(Comparator { p0, p1 ->
        var i = p0 - p1
        if (i == 0) i = 1
        return@Comparator i
    })*/

    //val sortedWith = list1.sortedWith(Comparator { p0, p1 -> if (p0 > p1) 1 else -1 })

    val treeSet = TreeSet<Int>(Comparator { p0, p1 ->
        var i = p0 - p1
        if (i == 0) i = -1
        return@Comparator i
    })
    treeSet.addAll(list2)



    //x > y
    //x > z
    //y > z






    println()


}

data class Data(
    val num: Int
)