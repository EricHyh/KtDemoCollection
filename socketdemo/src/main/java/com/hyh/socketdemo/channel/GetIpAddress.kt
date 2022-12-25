package com.hyh.socketdemo.channel

import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException
import java.util.*

/**
 * TODO
 *
 * @author eriche 2022/12/25
 */
object GetIpAddress {

    var IP: String? = null
    var PORT = 0

    val port: Int
        get() = PORT

    fun getLocalIpAddress(serverSocket: ServerSocket) {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    val mIP: String = inetAddress.hostAddress?.substring(0, 3) ?: ""
                    if (mIP == "192") {
                        IP = inetAddress.hostAddress //获取本地IP
                        PORT = serverSocket.localPort //获取本地的PORT
                        Log.e("ReceiveService", "" + IP)
                        Log.e("ReceiveService", "" + PORT)
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
    }
}
