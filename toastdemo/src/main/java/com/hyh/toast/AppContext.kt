package com.hyh.toast

import android.app.Application
import com.hyh.toast.api.FtToast

class AppContext : Application() {
    override fun onCreate() {
        super.onCreate()
        FtToast.init(this)
    }
}