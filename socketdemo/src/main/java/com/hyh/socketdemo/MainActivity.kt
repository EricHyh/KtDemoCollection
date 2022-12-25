package com.hyh.socketdemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hyh.socketdemo.channel.ReceiveService
import com.hyh.socketdemo.channel.SendService

/**
 * TODO
 *
 * @author eriche 2022/12/25
 */
class MainActivity : AppCompatActivity() {

    private val receiveService = ReceiveService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_start_receiver).setOnClickListener {
            receiveService.startService()
        }

        findViewById<View>(R.id.btn_send).setOnClickListener {
            SendService().send("发送数据测试")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        receiveService.stopService()
    }
}