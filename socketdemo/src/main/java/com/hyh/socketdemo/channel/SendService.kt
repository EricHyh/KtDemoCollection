package com.hyh.socketdemo.channel

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket

/**
 * TODO
 *
 * @author eriche 2022/12/25
 */
class SendService {


    fun startService() {}

    fun send(string: String) {
        Thread {
            //val socket = Socket("192.168.31.177", ChannelConstants.CHANNEL_PORT)
            val socket = Socket("127.0.0.1", ChannelConstants.CHANNEL_PORT)
            val outputStream = socket.getOutputStream()
            val bufferedWriter = BufferedWriter(OutputStreamWriter(outputStream, "utf-8"))
            bufferedWriter.write(string)
            bufferedWriter.close()
        }.start()
    }

    fun stopService() {}
}