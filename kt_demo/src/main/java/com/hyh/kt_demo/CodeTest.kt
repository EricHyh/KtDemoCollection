package com.hyh.kt_demo

import java.util.regex.Pattern


typealias Condition<E> = (E.() -> Boolean)


interface ElementWeightProvider<E> {

    val maxWeight: Long

    fun getWeight(element: E): Long

}

/**
 * 5 - 10 - 20
 *
 *
 * 1 - 19 - 20 - 21
 * 21 - 42 - 210
 *
 *
 * 0 - 20
 *
 *
 *
 * @param E
 * @param elementWeightProviders
 * @return
 */


fun <E> List<E>.sortedByWeightProvider(elementWeightProviders: List<ElementWeightProvider<E>>): List<E> {

    fun getWeight(element: E): Long {
        var weight = 0L
        var multiplier = 1L
        for (providerIndex in elementWeightProviders.indices.reversed()) {
            val elementWeightProvider = elementWeightProviders[providerIndex]
            weight += (elementWeightProvider.getWeight(element) + 1) * multiplier
            multiplier *= (elementWeightProvider.maxWeight + 1)
        }

        return weight
    }

    return this.sortedBy selector@{ element ->
        return@selector getWeight(element)
    }
}


fun <E> List<E>.sortedBy(priorityConditionsArray: Array<List<Condition<E>>>): List<E> {
    val elementWeightProviders = priorityConditionsArray.map { priorityConditions ->
        object : ElementWeightProvider<E> {

            override val maxWeight: Long = priorityConditions.size.toLong()

            override fun getWeight(element: E): Long {
                priorityConditions.forEachIndexed { index, condition ->
                    if (element.condition()) {
                        return@getWeight index.toLong()
                    }
                }
                return maxWeight
            }

        }
    }
    return sortedByWeightProvider(elementWeightProviders)
    /*fun getWeight(element: E): Long {
        var weight = 0L
        var multiplier = 1
        for (arrayIndex in priorityConditionsArray.indices.reversed()) {
            val priorityConditions = priorityConditionsArray[arrayIndex]
            for (index in priorityConditions.indices) {
                val condition = priorityConditions[index]
                if (element.condition()) {
                    weight += (index + 1) * multiplier
                    break
                } else if (index == priorityConditions.size - 1) {
                    weight += (priorityConditions.size + 1) * multiplier
                    break
                }
            }
            multiplier *= (priorityConditions.size + 1)
        }

        return weight
    }

    return this.sortedBy selector@{ element ->
        return@selector getWeight(element)
    }*/
}


fun <E> List<E>.sortedBy(priorityConditions: List<Condition<E>>): List<E> {
    /*return this.sortedBy selector@{ element ->
        priorityConditions.forEachIndexed { index, condition ->
            if (element.condition()) {
                return@selector index
            }
        }
        return@selector priorityConditions.size
    }*/
    return this.sortedBy(arrayOf(priorityConditions))
}


fun <E> List<E>.sortedBy(vararg priorityConditions: Condition<E>): List<E> {
    return sortedBy(priorityConditions.toList())
}


fun <E> List<E>.filter(conditions: List<Condition<E>>): List<E> {
    return filter { element ->
        conditions.find { condition ->
            element.condition()
        } != null
    }
}


fun main() {


    /*val p = Pattern.compile("([a-z]+)(\\d+)");
    val m = p.matcher("aaa2223bb");

    println(m.find()); // 匹配aaa2223
    println(m.groupCount()); // 返回2,因为有2组
    println(m.start(1)); // 返回0 返回第一组匹配到的子字符串的第一个字符在原字符串中的索引号
    println(m.start(2)); // 返回3 返回第二组匹配到的子字符串的第一个字符在原字符串中的索引号
    println(m.end(1)); // 返回3 返回第一组匹配到的子字符串的最后一个字符在原字符串中的索引号
    println(m.end(2)); // 返回7
    println(m.group(1)); // 返回aaa,返回第一组匹配到的子字符串
    println(m.group(2)); // 返回2223,返回第二组匹配到的子字符串*/


    val len = 4
    val compile = Pattern.compile("\\(\\d{" + len + "}\\)")
    //val compile = Pattern.compile("\\d")
    val matcher = compile.matcher("123(1234)456(2345)")

    //val start = matcher.start(groupCount - 1)

    matcher.find()
    //val groupCount = matcher.groupCount()
    val group1 = matcher.start()
    matcher.find()
    val group2 = matcher.start()


    println()

    val xx = listOf<Int>(1, 2, 3, 4).flatMapTo(
        LinkedHashSet(), object : ((Int) -> Iterable<String>) {
            override fun invoke(brokerId: Int): Iterable<String> {
                return when (brokerId) {
                    1 -> {
                        listOf("3", "2", "1")
                    }
                    2 -> {
                        listOf("4", "1", "3", "5")
                    }
                    3 -> {
                        listOf("5", "2", "6")
                    }
                    4 -> {
                        listOf("1", "7", "5", "8")
                    }
                    else -> {
                        emptyList<String>()
                    }
                }
            }
        }
    )



    println()


    val conditions = mutableListOf<Condition<String>>(
        { this.contains('a') },
        { this.contains('b') },
        { this.contains('c') },
        { this.contains('d') },
        { this.contains('e') },
    )

    conditions += { this.contains('a') }


    val sortedBy = listOf<String>(
        "kkkkk",
        "kkckk",
        "kkkebk",
        "kabck",
        "kakek",
        "kkdakk",
        "kkakk",
        "kkdkk",
        "kkcdkk",
        "kkckkk",
        "kkckk",
    ).sortedBy(
        { this.contains('b') },
        { this.contains('c') },
        { this.contains('d') },
        { this.contains('e') }
    )

    var s: String? = ""

    s.toString()

    val aClazz = AA::class.java


    aClazz?.let { }

    println("")

    "".toBigDecimalOrNull()


    val sortedBy1 = listOf<String>(
        "1kkkkk",
        "3kkckk",
        "6kkkebk",
        "2kabck",
        "10kakek",
        "4kkdakk",
        "4kkdakk",
        "7kkdakk",
        "1kkakk",
        "3kkdkk",
        "2kkcdkk",
        "3kkckkk",
        "1kkckk",
        "8kkckk",
    ).sortedBy(
        arrayOf(
            listOf<Condition<String>>(
                { this.contains('1') },
                { this.contains('2') },
                { this.contains('3') },
                { this.contains('4') }),

            listOf<Condition<String>>(
                { this.contains('a') },
                { this.contains('b') },
                { this.contains('c') },
                { this.contains('d') }),
        )
    )


    //MP3MusicPlayer("双截棍").play()

    val marketList =
        listOf("MARKET_HK", "MARKET_US", "MARKET_SG", "MARKET_JP", "MARKET_SH", "MARKET_SZ")
    //val marketList = listOf("MARKET_HK")
    val reduceIndexed = marketList.reduceIndexed { index, acc, s ->
        if (index == 1) {
            "($acc|$s"
        } else if (index == marketList.lastIndex) {
            "$acc|$s)"
        } else {
            "$acc|$s"
        }
    }

    val xx1 = "MARKET_HK".matches(Regex(reduceIndexed))
    val xx11 = "MARKET".matches(Regex(reduceIndexed))
    val xx2 = "MARKET_US".matches(Regex(reduceIndexed))
    val xx3 = "MARKET_SZ".matches(Regex(reduceIndexed))

    val regex = Regex(reduceIndexed)

    val test1 = "dfafafasff.MARKET_HK"
    val test2 = "dfafafasff.MARKET_HK.MARKET_HK"
    val test3 = "dfafafasff.xxxx.MARKET_HK"

    listOf(test1, test2, test3).forEach {
        val split = it.split(".")
        if (split.size >= 2) {
            val last = split.last()
            if (last.matches(regex)) {
                val substring = it.substring(0, it.lastIndexOf("."))
                println()
            }
        }
    }


    println()

}


enum class TestEnum {

    T1

}

class AA {

    private val a = 0

    private val x: Integer = Integer(0)

    var b = "0"

    var c: CC<Int>? = null

    var d: CC<DD<Int>>? = null

    var e: CC<DD<*>>? = null

    var f: List<Int>? = null

    var g: List<DD<*>>? = null

    var h: IntArray? = null

    var i: Array<List<Int>>? = null

    var j: TestEnum? = null

}


class CC<T>

class DD<T>

/*abstract class MusicPlayer {

    constructor() {
        val song = getSong()
        play(song)
    }

    abstract fun getSong(): String

    private fun play(song: String) {
        println(song)
    }
}

class MP3MusicPlayer : MusicPlayer {

    private var mSong: String = "夜曲"

    constructor(song: String) {
        mSong = song
    }

    override fun getSong(): String {
        return mSong
    }
}*/


/*abstract class MusicPlayer {

    fun play() {
        val song = getSong()
        println(song)
    }

    abstract fun getSong(): String
}

class MP3MusicPlayer : MusicPlayer {

    private var mSong: String = "夜曲"

    constructor(song: String) {
        mSong = song
    }

    override fun getSong(): String {
        return mSong
    }
}*/


interface IMusicDecoder {

    fun decode(song: String): String

}

class MP3MusicDecoder : IMusicDecoder {

    override fun decode(song: String): String {
        return "MP3解码${song}"
    }
}

class WAVMusicDecoder : IMusicDecoder {

    override fun decode(song: String): String {
        return "WAV解码${song}"
    }
}


abstract class MusicPlayer(decoder: IMusicDecoder) {

    private val mDecoder: IMusicDecoder = decoder


    fun play() {
        val song = getSong()
        println(mDecoder.decode(song))
    }

    abstract fun getSong(): String

}

