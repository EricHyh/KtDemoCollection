package com.hyh.toast

import android.app.Application
import android.util.Log
import org.json.JSONObject

class AppContext : Application() {
    override fun onCreate() {
        super.onCreate()


        val jsonObject = JSONObject()
        jsonObject.put("1", "99999999999")

        val jsonStr = jsonObject.toString()

        val json = JSONObject(jsonStr)
        val optInt = json.optInt("1", 110)
        Log.d("AppContext", "onCreate: $optInt")
    }
}