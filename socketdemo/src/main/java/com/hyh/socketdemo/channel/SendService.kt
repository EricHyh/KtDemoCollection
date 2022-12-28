package com.hyh.socketdemo.channel

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import channel_common_message.ChannelCommonMessage
import com.google.protobuf.GeneratedMessage
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 数据发送服务
 *
 * @author eriche 2022/12/25
 */
class SendService {

    companion object {

        init {
            GetIpAddress.initLocalIpAddress()
        }
    }

    private var started: AtomicBoolean = AtomicBoolean(false)

    private var handlerThread: HandlerThread? = null
    private var sendHandler: Handler? = null

    var receiveServiceIP: String? = null

    fun startService() {
        if (!started.compareAndSet(false, true)) return

        val handlerThread = HandlerThread("SendService").also { handlerThread = it }
        handlerThread.start()
        sendHandler = Handler(handlerThread.looper, SendHandlerCallback())
    }

    fun send(message: ChannelCommonMessage.ChannelCommonInfo) {
        if (!started.get()) return
        val receiveServiceIP = receiveServiceIP ?: return
        sendHandler?.sendMessage(Message.obtain().apply { obj = MessageData(receiveServiceIP, message) })
    }

    fun stopService() {
        if (!started.compareAndSet(true, false)) return

        handlerThread?.quitSafely()
        handlerThread = null
        sendHandler = null
    }

    private inner class SendHandlerCallback : Handler.Callback {

        override fun handleMessage(message: Message): Boolean {
            val messageData = message.obj as? MessageData ?: return true
            var socket: Socket? = null
            var outputStream: OutputStream? = null
            try {
                socket = Socket(messageData.receiveServiceIP, ChannelConstants.CHANNEL_PORT)
                outputStream = socket.getOutputStream()
                messageData.message.writeTo(outputStream)
                StreamUtils.closeSafety(outputStream, socket)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                StreamUtils.closeSafety(outputStream, socket)
            }
            return true
        }
    }

    data class MessageData(
        val receiveServiceIP: String,
        val message: GeneratedMessage,
    )
}