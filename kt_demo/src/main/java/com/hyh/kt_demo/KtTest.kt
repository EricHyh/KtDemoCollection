package com.hyh.kt_demo

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/4/27
 */
abstract class TextView {

    init {
        println("TextView init")
        /*val decoder = getText()
        onTextChanged(decoder)*/
    }

    constructor() {
        println("TextView constructor")
    }

    abstract fun getText(): String

    open fun onTextChanged(newText: String) {
        println("TextView onTextChanged: $newText")
    }
}


fun main() {
    val create = create<IListEvent>()
    print("" + create)
}

interface IEvent

interface IListEvent : IEvent

inline fun <reified T : IEvent> create(): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java), EventInvocationHandler()) as T
}

class EventInvocationHandler() : InvocationHandler {

    override fun invoke(p0: Any?, p1: Method?, p2: Array<out Any>?): Any {
        return ""
    }
}
