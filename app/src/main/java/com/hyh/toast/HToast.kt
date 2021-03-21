package com.hyh.toast

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.hyh.demo.R

object HToast {

    fun show(context: Context) {
        //Toast.makeText(context, "测试Toast", Toast.LENGTH_LONG).show()
        val toast = Toast(context)
        val view = LayoutInflater.from(context).inflate(R.layout.view_toast, null)
        toast.view = view
        toast.duration = Toast.LENGTH_LONG
        toast.show()
    }


}