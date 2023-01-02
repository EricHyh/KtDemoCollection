package com.hyh.socketdemo.channel

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.hyh.socketdemo.channel.message.ChannelCommonMessage
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
        private const val TAG = "SendService"
    }

    /**
     * 服务是否已启动
     */
    private var started: AtomicBoolean = AtomicBoolean(false)

    /**
     * 消息发送线程（单线程处理）
     */
    private var handlerThread: HandlerThread? = null
    private var sendHandler: Handler? = null

    /**
     * 接收服务ID
     */
    var receiveServiceIP: String? = null

    /**
     * 开启服务
     */
    fun startService() {
        if (!started.compareAndSet(false, true)) return

        Log.d(TAG, "startService: receiveServiceIP=${receiveServiceIP}")

        val handlerThread = HandlerThread("SendService").also { handlerThread = it }
        handlerThread.start()
        sendHandler = Handler(handlerThread.looper, SendHandlerCallback())
    }

    /**
     * 发送消息给接收服务
     */
    fun send(message: ChannelCommonMessage.ChannelCommonInfo) {
        if (!started.get()) return
        val receiveServiceIP = receiveServiceIP ?: return Unit.also {
            Log.d(TAG, "send: receiveServiceIP is null")
        }
        Log.d(TAG, "send: receiveServiceIP=${receiveServiceIP}")
        sendHandler?.sendMessage(
            Message.obtain().apply { obj = MessageData(receiveServiceIP, message) })
    }

    /**
     * 结束服务
     */
    fun stopService() {
        if (!started.compareAndSet(true, false)) return

        Log.d(TAG, "stopService: receiveServiceIP=${receiveServiceIP}")

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
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                StreamUtils.closeSafety(outputStream, socket)
            }
            return true
        }
    }

    private data class MessageData(
        val receiveServiceIP: String,
        val message: GeneratedMessage,
    )
}