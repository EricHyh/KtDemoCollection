package com.hyh.socketdemo.channel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

/**
 * 读取设备本地IP
 *
 * @author eriche 2022/12/25
 */
object GetIpAddress {

    private const val TAG = "GetIpAddress"

    private val mutableLocalIP: MutableLiveData<String?> = MutableLiveData()
    val localIP: LiveData<String?>
        get() = mutableLocalIP

    fun initLocalIpAddress() {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val element: NetworkInterface = en.nextElement()
                val inetAddresses: Enumeration<InetAddress> = element.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress: InetAddress = inetAddresses.nextElement()
                    val ip: String = inetAddress.hostAddress?.substring(0, 3) ?: ""
                    if (inetAddress.isSiteLocalAddress) {
                        mutableLocalIP.postValue(ip)
                        return
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
    }
}
