package com.hyh.activity

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hyh.demo.R
import com.hyh.toast.HToast

class ToastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toast)
    }

    fun showToast(view: View) {
        Handler().postDelayed({
            HToast.show(this)
        }, 2000)
    }
}