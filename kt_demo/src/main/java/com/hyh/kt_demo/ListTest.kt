package com.hyh.kt_demo

import net.sourceforge.pinyin4j.PinyinHelper
import java.text.Collator
import java.util.*
import kotlin.math.sign


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

    val list2 = listOf<Int>(
        0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 3
    )
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


    //val list = listOf<String>("骏利亨德森环", "骏利亨德森平", "路博迈NB美果","资本集体日本","资本集体新世")
    //val list = listOf<String>("路博迈NB美果", "资本集体新世", "骏利亨德森环","资本集体日本","骏利亨德森平","安全","逼停")
    val list = listOf<String>("路博", "资本", "骏利","资本","骏利","安全","逼停")
    val list1 = listOf<String>("路", "资", "骏","资","骏","安","逼")

    val sorted = list.sorted().reversed()
    val sorted1 = list1.sorted().reversed()

    val compareTo = "安".compareTo("骏")

    val compareTo1 = "骏".compareTo("逼")
    val compareTo2 = "资".compareTo("逼")
    val compareTo3 = "路".compareTo("逼")
    val compareTo4 = "资".compareTo("被")
    val compareTo5 = "路".compareTo("被")



    val xx = TreeSet<String>(kotlin.Comparator { t, t2 ->
        return@Comparator sign(t.compareTo(t2).toDouble()).toInt()
    })

    xx.addAll(listOf<String>("骏利亨德森环", "骏利亨德森平", "路博迈NB美果","资本集体日本","资本集体新世"))
    val sortedWith = listOf<String>("骏利亨德森环", "骏利亨德森平", "hello", "A-15","14","A","zgood","Z","路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(Comparator { t, t2 ->
        return@Comparator sign(t.compareTo(t2).toDouble()).toInt()
    })

    val sortedWith1 = listOf<String>("骏利亨德森环", "骏利亨德森平", "hello", "A-15","14","A","zgood","Z","路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(PinyinComparator())


    val sortedWith2 = listOf<String>("骏利亨德森环", "骏利亨德森平", "hello", "A-15","14","A","zgood","Z","路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(Collator.getInstance(Locale.ENGLISH))

    val sortedWith22 = listOf<String>("骏利亨德森环", "骏利亨德森平", "hello", "A-15","14","A","zgood","Z","路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith( Collator.getInstance(Locale.CHINA))

    val sortedWith222 = listOf<String>("骏利亨德森环", "骏利亨德森平", "hello", "A-15","14","A","zgood","Z","路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(Collator.getInstance(Locale.CHINESE))

    val sortedWith3 = listOf<String>( "hello", "A-15","14","A","zgood","Z")
        .sortedWith( Collator.getInstance(Locale.CHINESE))

    val sortedWith4 = listOf<String>( "hello", "A-15","14","A","zgood","Z")
        .sortedWith( Collator.getInstance(Locale.ENGLISH))

    val sortedWith5 = listOf<String>( "hello", "A-15","14","A","zgood","Z")
        .sortedWith( Comparator { t, t2 ->
            return@Comparator sign(t.compareTo(t2).toDouble()).toInt()
        })


    val sortedWith6 = listOf<Int>(5, 100, 0, 50, 20, 12).sortedWith(kotlin.Comparator { t, t2 ->
        return@Comparator t.compareTo(t2)
    })





    val compareTo6 =  sign("资本集体日本".compareTo("路博迈NB美果").toDouble()).toInt()
    val compareTo66 =  sign("资".compareTo("路").toDouble()).toInt()
    val compareTo666 =  sign("资".compareTo("安").toDouble()).toInt()
    val compareTo7 = 5.compareTo(100)


    println()


}



/**
 * 汉字按照拼音排序的比较器
 * @author KennyLee 2009-2-23 10:08:59
 */
class PinyinComparator : Comparator<Any> {

    override fun compare(o1: Any, o2: Any): Int {
        val c1 = (o1 as String)[0]
        val c2 = (o2 as String)[0]
        return concatPinyinStringArray(
            PinyinHelper.toHanyuPinyinStringArray(c1)
        ).compareTo(
            concatPinyinStringArray(
                PinyinHelper
                    .toHanyuPinyinStringArray(c2)
            )
        )
    }

    private fun concatPinyinStringArray(pinyinArray: Array<String>?): String {
        val pinyinSbf = StringBuffer()
        if (pinyinArray != null && pinyinArray.size > 0) {
            for (i in pinyinArray.indices) {
                pinyinSbf.append(pinyinArray[i])
            }
        }
        return pinyinSbf.toString()
    }
}

data class Data(
    val num: Int
)