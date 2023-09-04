package com.hyh.socketdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.hyh.socketdemo.channel.message.ChannelCommonMessage
import com.google.protobuf.ByteString
import com.hyh.socketdemo.channel.ReceiveListener
import com.hyh.socketdemo.channel.ReceiveService
import com.hyh.socketdemo.channel.SendService
import test_message.TestMessage

/**
 * TODO
 *
 * @author eriche 2022/12/25
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val receiveService = ReceiveService().apply {
        receiveListener = object : ReceiveListener {

            override fun onReceived(commonInfo: ChannelCommonMessage.ChannelCommonInfo) {

                val toByteArray = commonInfo.data.toByteArray()
                val info = TestMessage.XXXTestMessageInfo2.parseFrom(toByteArray)

                Log.d(TAG, "onReceived: ${commonInfo.cmd} - ${info.str2}")
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "onError: $throwable")
            }
        }
    }

    private val sendService = SendService().apply {
        receiveServiceIP = "127.0.0.1"
    }

    private lateinit var test: String

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate: ")

        findViewById<View>(R.id.btn_start_receiver).setOnClickListener {
            receiveService.startService()
        }
        findViewById<View>(R.id.btn_start_sender).setOnClickListener {
            sendService.startService()
        }

        findViewById<View>(R.id.btn_send).setOnClickListener {
            sendService.send(
                ChannelCommonMessage.ChannelCommonInfo.newBuilder()
                    .setCmd(1)
                    .setData(
                        ByteString.copyFrom(
                            TestMessage.TestMessageInfo.newBuilder()
                                .setFlag(true)
                                .setNum(10)
                                .setStr("哈哈12as")
                                .setSubMessageInfo(
                                    TestMessage
                                        .TestSubMessageInfo
                                        .newBuilder()
                                        .setSubFlag(true)
                                        .setSubNum(100)
                                        .setSubStr("setSubStr")
                                )
                                .build().toByteArray()
                        )
                    )
                    .build()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        receiveService.stopService()
        sendService.stopService()
    }


    private fun getCurrentRoot(): FrameLayout? {
        this.let {
            var root: View? = it.findViewById(androidx.appcompat.R.id.action_bar_root)
            if (root != null) {
                val parent = root.parent
                if (parent != null && parent is FrameLayout) {
                    return parent
                }
            } else {
                root = it.findViewById(android.R.id.content)
                if (root is FrameLayout) {
                    return root
                }
            }
        }
        return null
    }
}