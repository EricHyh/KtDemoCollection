package com.hyh.socketdemo.channel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hyh.socketdemo.channel.message.ChannelCommonMessage
import com.hyh.socketdemo.channel.StreamUtils.closeSafety
import java.io.InputStream
import java.net.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 数据接收服务
 *
 * @author eriche 2022/12/25
 */
class ReceiveService {

    companion object {

        private const val TAG = "ReceiveService"

        /**
         * 发送服务IP
         */
        private val mutableLocalIP: MutableLiveData<String?> = MutableLiveData()
        val localIP: LiveData<String?>
            get() = mutableLocalIP

        /**
         * 初始化设备本地IP
         */
        private fun initLocalIpAddress() {
            try {
                val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val element: NetworkInterface = en.nextElement()
                    val inetAddresses: Enumeration<InetAddress> = element.inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val inetAddress: InetAddress = inetAddresses.nextElement()
                        val hostAddress = inetAddress.hostAddress
                        if (!hostAddress.isNullOrEmpty() && inetAddress.isSiteLocalAddress) {
                            mutableLocalIP.postValue(hostAddress)
                            Log.d(TAG, "initLocalIpAddress: $hostAddress")
                            return
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
        }

        init {
            initLocalIpAddress()
        }
    }

    /**
     * 消息接收监听
     */
    var receiveListener: ReceiveListener? = null

    /**
     * 消息接收处理线程池
     */
    private var executor: ThreadPoolExecutor? = null

    /**
     * 服务是否启动
     */
    private val started: AtomicBoolean = AtomicBoolean(false)

    /**
     * 服务失败重试次数
     */
    private val retryCount: AtomicInteger = AtomicInteger()


    /**
     * 接收服务套接字
     */
    private var serverSocket: ServerSocket? = null
    private val serverSocketLock: Any = Any()

    /**
     * 开启服务
     */
    fun startService() {
        if (!started.compareAndSet(false, true)) return

        Log.d(TAG, "startService: ${mutableLocalIP.value}")

        val executor = createThreadPool().also { executor = it }

        executor.execute(createAndRunService(executor))
    }

    /**
     * 结束服务
     */
    fun stopService() {
        if (!started.compareAndSet(true, false)) return

        Log.d(TAG, "stopService: ${mutableLocalIP.value}")

        retryCount.set(0)

        serverSocket?.let {
            releaseServerSocket(it)
        }

        executor?.shutdownNow()
        executor = null
    }


    private fun createServerSocket(): ServerSocket {
        val serverSocket = ServerSocket(ChannelConstants.CHANNEL_PORT)
        synchronized(serverSocketLock) {
            this.serverSocket = serverSocket
        }
        return serverSocket
    }

    private fun releaseServerSocket(serverSocket: ServerSocket?) {
        synchronized(serverSocketLock) {
            if (this.serverSocket === serverSocket) {
                this.serverSocket = null
            }
        }
        serverSocket.closeSafety()
    }

    private fun retryService() {
        if (started.get()) {
            val executor = createThreadPool().also { executor = it }
            executor.execute(createAndRunService(executor))
        }
    }

    private fun createAndRunService(executor: ThreadPoolExecutor): Runnable {
        return Runnable {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = createServerSocket()
                while (started.get() && !executor.isShutdown) {
                    val accept: Socket? = serverSocket.accept()
                    val receiveTask = ReceiveTask(accept, receiveListener)
                    executor.execute(receiveTask)
                    retryCount.set(0)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                releaseServerSocket(serverSocket)
                kotlin.runCatching {
                    Thread.sleep((500 * retryCount.incrementAndGet()).toLong())
                }
                retryService()
            }
        }
    }

    private fun createThreadPool(): ThreadPoolExecutor {

        fun createThreadFactory(): ThreadFactory {
            return ThreadFactory { runnable ->
                val result = Thread(runnable, TAG)
                result.isDaemon = true
                result
            }
        }

        return ThreadPoolExecutor(
            1, 4, 120, TimeUnit.SECONDS,
            SynchronousQueue(), createThreadFactory()
        )
    }

    /**
     * 消息接收处理任务
     */
    private class ReceiveTask(
        private val accept: Socket?,
        private val receiveListener: ReceiveListener?
    ) : Runnable {

        override fun run() {
            if (accept == null) {
                receiveListener?.onError(NullPointerException("accept is null"))
                return
            }
            var inputStream: InputStream? = null
            try {
                inputStream = accept.getInputStream()
                val commonInfo = ChannelCommonMessage.ChannelCommonInfo.parseFrom(inputStream)
                closeSafety(inputStream, accept)
                receiveListener?.onReceived(commonInfo)
            } catch (e: Throwable) {
                receiveListener?.onError(e)
            } finally {
                closeSafety(inputStream, accept)
            }
        }
    }
}


interface ReceiveListener {

    fun onReceived(commonInfo: ChannelCommonMessage.ChannelCommonInfo)

    fun onError(throwable: Throwable)

}