package com.hyh.coroutine

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * 使用[ConflatedBroadcastChannel]实现[MutableStateFlow]的功能
 *
 * @author eriche
 * @data 2021/6/21
 */
internal class SimpleMutableStateFlow<T : Any>(initialValue: T) {

    private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel(initialValue)

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
        synchronized(valueRef) {
            if (oldValue == newValue) return
            if (!channel.isClosedForSend) {
                channel.offer(newValue)
                valueRef.set(newValue)
            }
        }
    }
}