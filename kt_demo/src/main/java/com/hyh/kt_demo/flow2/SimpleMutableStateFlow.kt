package com.hyh.kt_demo.flow2

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

/**
 * 使用[ConflatedBroadcastChannel]实现[MutableStateFlow]的功能
 *
 * @author eriche
 * @data 2021/6/21
 */
class SimpleMutableStateFlow<T>(initialValue: T) {

    private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel(initialValue)

    private val valueRef = AtomicReference(initialValue)

    private val safeValue: T
        get() = channel.valueOrNull ?: valueRef.get()

    var value: T
        get() = safeValue
        set(value) {
            updateValue(safeValue, value)
        }

    private fun updateValue(oldValue: T, newValue: T) {
        synchronized(valueRef) {
            if (oldValue == newValue) return
            if (!channel.isClosedForSend) {
                channel.runCatching {
                    channel.trySend(newValue)
                    valueRef.set(newValue)
                }
            }
        }
    }

    fun asStateFlow(): SimpleStateFlow<T> {
        return SimpleStateFlowImpl(this)
    }

    fun close() {
        channel.runCatching {
            channel.close()
        }
    }

    class SimpleStateFlowImpl<T>(private val stateFlow: SimpleMutableStateFlow<T>) : SimpleStateFlow<T> {

        override val value: T
            get() = stateFlow.value

        @InternalCoroutinesApi
        override suspend fun collect(collector: FlowCollector<T>) {
            stateFlow.channel.openSubscription().receiveAsFlow().collect(collector)
        }
    }
}

interface SimpleStateFlow<out T> : Flow<T> {

    val value: T

}