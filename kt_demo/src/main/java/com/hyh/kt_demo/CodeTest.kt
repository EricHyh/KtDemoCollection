package com.hyh.kt_demo


fun main() {
    //MP3MusicPlayer("双截棍").play()

    val marketList = listOf("MARKET_HK", "MARKET_US", "MARKET_SG", "MARKET_JP", "MARKET_SH", "MARKET_SZ")
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

