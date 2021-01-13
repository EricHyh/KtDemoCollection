package com.hyh.kt_demo.event

import com.hyh.kt_demo.flow1.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.collections.HashMap

/**
 * TODO: Add Description
 *
 * 1.可读性
 * 2.拓展性
 * 3.可用性
 * 4.约束性
 * <br>
 *
 *
 * @author eriche
 * @data 2020/12/1
 */
interface EventChannelFactory {
}

interface ClickEventFactory<T> : EventChannelFactory {
    fun getClickEventChannel(): IEventChannel<T>
}

interface DeleteEventFactory<T> : EventChannelFactory {
    fun getDeleteEventChannel(): IEventChannel<T>
}

interface ClickAndDeleteEventFactory<T1, T2> : ClickEventFactory<T1>, DeleteEventFactory<T2>


inline fun <reified T : EventChannelFactory> EventChannelFactory.asTyped(): T? {
    return if (this is T) this else null
}

inline fun <reified T : EventChannelFactory> newEventChannelFactory(): T {
    return Proxy.newProxyInstance(
        EventChannelFactory::class.java.classLoader,
        arrayOf(T::class.java),
        EventInvocationHandler()
    ) as T
}

interface IEventChannel<T> {

    fun send(t: T)

    fun asFlow(): Flow<T>

}

class EventData(val data: Any? = null) {
    inline fun <reified T> getTypedData(): T? {
        return if (data is T) {
            data as T
        } else {
            null
        }
    }
}

class EventInvocationHandler : InvocationHandler {

    companion object {
        //用于保证返回值为基础数据类型时，不返回null
        private val PRIMITIVE_DEFAULT_VALUE: MutableMap<Class<*>?, Any> = HashMap(8)

        init {
            PRIMITIVE_DEFAULT_VALUE[Byte::class.javaPrimitiveType] = 0.toByte()
            PRIMITIVE_DEFAULT_VALUE[Short::class.javaPrimitiveType] = 0.toShort()
            PRIMITIVE_DEFAULT_VALUE[Int::class.javaPrimitiveType] = 0
            PRIMITIVE_DEFAULT_VALUE[Long::class.javaPrimitiveType] = 0L
            PRIMITIVE_DEFAULT_VALUE[Float::class.javaPrimitiveType] = 0.0f
            PRIMITIVE_DEFAULT_VALUE[Double::class.javaPrimitiveType] = 0.0
            PRIMITIVE_DEFAULT_VALUE[Boolean::class.javaPrimitiveType] = false
            PRIMITIVE_DEFAULT_VALUE[Char::class.javaPrimitiveType] = '\u0000'
        }
    }

    val map: HashMap<String, IEventChannel<*>> = HashMap()

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
        val returnType = method.returnType
        val genericReturnType = method.genericReturnType

        if (returnType == IEventChannel::class.java) {
            if (map[method.name] != null) return map[method.name]
            map[method.name] = InnerEventChannel()
            return map[method.name]
        }

        return PRIMITIVE_DEFAULT_VALUE[returnType]
    }

    class InnerEventChannel : IEventChannel<Any?> {

        val flow: MutableStateFlow<Any?> = MutableStateFlow(1)

        override fun send(t: Any?) {
            flow.value = t
        }

        override fun asFlow(): Flow<Any?> {
            return flow
        }
    }
}

fun main() {
    val factory = newEventChannelFactory<ClickAndDeleteEventFactory<Int, String>>()

    GlobalScope.launch {
        val asFlow = factory.getClickEventChannel()
            .asFlow()
        asFlow
            .collect {
                log("collect : $it")
            }
    }

    Thread.sleep(1000)

    runBlocking {
        factory.getClickEventChannel().send(10)
    }

    Thread.sleep(10000)
}
