package com.hyh.kt_demo


typealias Condition<E> = (E.() -> Boolean)


fun <E> List<E>.sortedBy(priorityConditionsArray: Array<List<Condition<E>>>): List<E> {

    fun getWeight(element: E): Int {
        var weight = 0
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
    }
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
            listOf(
                { this.contains('1') },
                { this.contains('2') },
                { this.contains('3') },
                { this.contains('4') }),

            listOf(
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

