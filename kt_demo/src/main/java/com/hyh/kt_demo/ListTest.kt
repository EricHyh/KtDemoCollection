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

    /*val sortedWith1 = listOf<String>("骏利亨德森环", "骏利亨德森平", "Wells", "wells", "hello", "A-15", "14", "A", "zgood", "Z", "路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(PinyinComparator())


    val sortedWith2 = listOf<String>("骏利亨德森环", "骏利亨德森平", "Wells美国", "wells", "hello", "A-15", "14", "A", "zgood", "Z", "路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(Collator.getInstance(Locale.ENGLISH))

    val sortedWith22 = listOf<String>("骏利亨德森环", "骏利亨德森平", "Wells美国", "wells", "A-15", "14", "A", "zgood", "Z", "路博迈NB美果", "资本集体日本", "资本集体新世")
        .sortedWith(Collator.getInstance(Locale.CHINA))

    val sortedWith222 =
        listOf<String>("骏利亨德森环", "骏利亨德森平", "Wells美国", "wells", "hello", "A-15", "14", "A", "zgood", "Z", "路博迈NB美果", "资本集体日本", "资本集体新世")
            .sortedWith(Collator.getInstance(Locale.CHINESE))*/

    val s = "4761.499319"


    println("${s.toIntOrNull()}")

    val sortedWith1 = listOf<String>(
        "骏利亨德森环",
        "骏利亨德森平",
        "Wells Fargo美国投资",
        "安本标准中国",
        "路博迈NB美果",
        "资本集体日本",
        "资本集体新世")
        .sortedWith(Collator.getInstance(Locale.ENGLISH))

    val sortedWith2 = listOf<String>(
        "骏利亨德森环",
        "骏利亨德森平",
        "Wells Fargo美国投资",
        "安本标准中国",
        "路博迈NB美果",
        "资本集体日本",
        "资本集体新世")
        .sortedWith(Collator.getInstance(Locale.CHINESE))


    val compare1 = Collator.getInstance(Locale.CHINESE).compare("Wells Fargo美国投资级别信贷基金", "安本标准中国A股股票基金")
    val compare2 = Collator.getInstance(Locale.CHINESE).compare("安本标准中国A股股票基金", "安联环球人工智能股票基金")
    val compare3 = Collator.getInstance(Locale.CHINESE).compare("Wells Fargo美国投资级别信贷基金", "安联环球人工智能股票基金")
    val compare4 = Collator.getInstance(Locale.CHINESE).compare("Wells Fargo美国各类型市值股票增长基金", "Wells Fargo美国投资级别信贷基金")


    val treeSet = TreeSet<String>(kotlin.Comparator { t, t2 ->
        return@Comparator Collator.getInstance(Locale.CHINESE).compare(t, t2)
    })
    treeSet.add("Wells Fargo美国投资级别信贷基金")
    treeSet.add("Wells Fargo美国各类型市值股票增长基金")
    treeSet.add("天利(卢森堡)全球焦点基金")
    //treeSet.add("安本标准中国A股股票基金")
    //treeSet.add("安联环球人工智能股票基金")


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