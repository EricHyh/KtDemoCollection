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
    print("main ${3 / 2}")


    listOf<ITest3>(ITest21(), ITest22(), ITest23())
        .forEach {
            val iTest2 = it as ITest2<ITest1>
            print("")
        }
}


interface ITest1
interface ITest2<T : ITest1> {
    fun t(t: T)
}
typealias ITest3 = ITest2<out ITest1>


class ITest11 : ITest1
class ITest12 : ITest1
class ITest13 : ITest1


class ITest21 : ITest2<ITest11> {
    override fun t(t: ITest11) {
    }
}

class ITest22 : ITest2<ITest12> {
    override fun t(t: ITest12) {
    }
}

class ITest23 : ITest2<ITest13> {
    override fun t(t: ITest13) {
    }
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
