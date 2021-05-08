package com.hyh.kt_demo


fun main() {
    //MP3MusicPlayer("双截棍").play()
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

