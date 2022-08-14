package com.hyh.kt_demo.coroutine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * 事件管理简单实现
 *
 * @author eriche
 * @data 2021/6/21
 */
@ExperimentalCoroutinesApi
internal class SimpleMutableStateFlow<T : Any>(initialValue: T) {

    private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel(initialValue)

    //private val state = MutableStateFlow(Pair(Integer.MIN_VALUE, initialValue))

    private val valueRef = AtomicReference(initialValue)

    private val safeValue: T
        get() = channel.valueOrNull ?: valueRef.get()

    var value: T
        get() = safeValue
        set(value) {
            updateValue(safeValue, value)
        }

    val flow = channel.openSubscription().consumeAsFlow()

    private fun updateValue(oldValue: T, newValue: T) {
        //state.value = Pair(state.value.first + 1, data)
        synchronized(valueRef) {
            if (oldValue == newValue) return
            if (!channel.isClosedForSend) {
                channel.trySend(newValue)
                valueRef.set(newValue)
            }
            //valueRef.compareAndSet()
        }
    }

    fun close() {
        channel.close()
    }
}