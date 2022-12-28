package com.hyh.socketdemo.channel

import channel_common_message.ChannelCommonMessage
import com.hyh.socketdemo.channel.StreamUtils.closeSafety
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 数据接收服务
 *
 * @author eriche 2022/12/25
 */
class ReceiveService {

    companion object {

        private const val TAG = "ReceiveService"

        init {
            GetIpAddress.initLocalIpAddress()
        }
    }

    var receiveListener: ReceiveListener? = null

    private var executor: ThreadPoolExecutor? = null

    private var started: AtomicBoolean = AtomicBoolean(false)

    private var serverSocket: ServerSocket? = null

    fun startService() {
        if (!started.compareAndSet(false, true)) return

        val executor = ThreadPoolExecutor(
            1, Int.MAX_VALUE, 120, TimeUnit.SECONDS,
            SynchronousQueue(), createThreadFactory()
        ).also { executor = it }

        executor.execute {
            val serverSocket = ServerSocket(ChannelConstants.CHANNEL_PORT).also { serverSocket = it }
            while (started.get() && !executor.isShutdown) {
                val accept = serverSocket.accept()
                val receiveTask = ReceiveTask(accept, receiveListener)
                executor.execute(receiveTask)
            }
        }
    }

    fun stopService() {
        if (!started.compareAndSet(true, false)) return

        serverSocket?.closeSafety()
        serverSocket = null
        executor?.shutdownNow()
        executor = null
    }

    private fun createThreadFactory(): ThreadFactory {
        return ThreadFactory { runnable ->
            val result = Thread(runnable, TAG)
            result.isDaemon = true
            result
        }
    }

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