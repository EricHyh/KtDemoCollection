package com.hyh.adapter

import android.util.Log

enum class TestEn(override val id: String) : ITestEn {

    AAA("1") {
        override fun append() {
            Log.d("TestEn", "$this.name : ${this.id}")
        }

    },

    BBB("2") {
        override fun append() {
            Log.d("TestEn", "$this.name : ${this.id}")

        }
    }
}

abstract class MainTestEn : ITestEn {

    override fun append() {
        Log.d("MainTestEn", "$this.name : ${this.id}")
    }
}


interface ITestEn {

    val id: String

    fun append()

}


object XXX {

    @JvmStatic
    fun test(call: () -> Unit) {
        call()
    }

    @JvmStatic
    fun test(call: (aa: Any) -> String) {

    }
}


