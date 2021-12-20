package com.hyh.kt_demo

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.awaitAll
import java.awt.SystemColor
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.collections.LinkedHashMap

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

val lock = Any()

fun main() {


    Observable.fromIterable(listOf(0, 1, 2, 3))
        .flatMap { int1 ->
            Observable.range(0, 5)
                .observeOn(Schedulers.io())
                .map { int2 ->
                    val result = "$int1 - $int2"
                    println("map ${Thread.currentThread().name} - $result")
                    result
                }
                .doOnNext {
                    println("doOnNext ${Thread.currentThread().name} - $it")
                }
        }
        .subscribe {

        }

    mutableListOf<Int>().removeFirstOrNull()




    /*val linkedHashMap = LinkedHashMap<Int, String>(8, 0.75F, true)

    linkedHashMap.put(1, "1")
    linkedHashMap.put(2, "2")
    linkedHashMap.put(3, "3")
    linkedHashMap.put(4, "4")
    linkedHashMap.put(5, "5")
    linkedHashMap.put(6, "6")
    linkedHashMap.put(7, "7")
    linkedHashMap.put(8, "8")
    linkedHashMap.put(9, "9")
    linkedHashMap.put(10, "10")
    linkedHashMap.put(1, "11")
    linkedHashMap.put(2, "22")
    linkedHashMap.put(3, "33")

    linkedHashMap.entries.forEach {
        println("key:${it.key}, value:${it.value}")
    }

    linkedHashMap.entries.lastOrNull()

    val create = create<IListEvent>()
    print("main ${3 / 2}")


    listOf<ITest3>(ITest21(), ITest22(), ITest23())
        .forEach {
            val iTest2 = it as ITest2<ITest1>
            print("")
        }*/
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
