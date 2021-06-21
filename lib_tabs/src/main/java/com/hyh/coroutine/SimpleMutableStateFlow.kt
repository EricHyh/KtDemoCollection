package com.hyh.coroutine

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow

/**
 * 事件管理简单实现
 *
 * @author eriche
 * @data 2021/6/21
 */
internal class SimpleMutableStateFlow<T : Any>(initialValue: T) {

    private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel(initialValue)

    //private val state = MutableStateFlow(Pair(Integer.MIN_VALUE, initialValue))

    var value: T
        get() = channel.value
        set(value) {
            send(value)
        }

    val flow = channel.asFlow()

    private fun send(data: T) {
        channel.offer(data)
        //state.value = Pair(state.value.first + 1, data)
    }
}