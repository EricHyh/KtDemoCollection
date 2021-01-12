package com.hyh.feeds

import kotlinx.coroutines.flow.Flow

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

inline fun <reified T : EventChannelFactory> createEventChannelFactory(): T {
    return null!!
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