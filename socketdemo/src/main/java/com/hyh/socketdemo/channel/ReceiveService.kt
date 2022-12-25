package com.hyh.socketdemo.channel

import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TODO
 *
 * @author eriche 2022/12/25
 */
class ReceiveService {

    companion object {
        private const val TAG = "ReceiveService"
    }

    private var started: AtomicBoolean = AtomicBoolean(false)

    fun startService() {
        if (!started.compareAndSet(false, true)) return

        //val address = InetSocketAddress("192.168.31.166", 8888)
        val address = Inet4Address.getByName("127.0.0.1")
        val serverSocket = ServerSocket( ChannelConstants.CHANNEL_PORT,50, address)



//        val addr: InetAddress = Inet4Address.getByAddress(byteArrayOf(127, 0, 0, 1))
//        val serverSocket = ServerSocket(8080, 50, addr)


        GetIpAddress.getLocalIpAddress(serverSocket)
        Thread {
            while (started.get()) {
                val accept = serverSocket.accept()
                val inputStream = accept.getInputStream()
                val baos = ByteArrayOutputStream()
                var readLen: Int
                val buffer = ByteArray(1024)

                while ((inputStream.read(buffer).also { readLen = it }) != -1) {
                    baos.write(buffer, 0, readLen)
                }

                baos.close()
                inputStream.close()

                val byteArray = baos.toByteArray()
                Log.d(TAG, "startService: ${byteArray.decodeToString()}")

            }
        }.start()
    }


    fun stopService() {
        started.set(false)
    }
}