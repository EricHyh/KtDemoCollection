package com.hyh.event

import androidx.lifecycle.LifecycleOwner


interface ISpecifiedEventChannel<T : IEvent> {

    fun send(t: T)

    //fun receiveAsFlow(coroutine: CoroutineContext? = null): Flow<T>

    fun observe(owner: LifecycleOwner? = null, observer: (data: T) -> Unit)

}

interface IEventChannel<T : IEvent> {

    fun observe(vararg eventTypes: Class<out T>, observer: (event: T) -> Unit)
    fun observeAll(observer: (event: T) -> Unit)
    fun send(event: T)

}


interface INewEventChannel<SEND : IEvent, RECEIVE : IEvent> {

    fun getSender(): IEventSender<SEND>

    fun getReceiver(): IEventReceiver<RECEIVE>

}


interface IEventReceiver<T : IEvent> {
    fun observe(vararg eventTypes: Class<out T>, observer: (event: T) -> Unit)
    fun observeAll(observer: (event: T) -> Unit)
}

interface IEventSender<T : IEvent> {
    fun send(event: T)
}


/*interface INewEventChannel<SEND : IEvent, RECEIVE : IEvent> {

    fun observe(vararg eventTypes: Class<out RECEIVE>, observer: (event: RECEIVE) -> Unit)
    fun observeAll(observer: (event: RECEIVE) -> Unit)
    fun send(event: SEND)

}


interface IEventChannelFactory<E1 : IEvent, E2 : IEvent> {

    getEventChannelForList


}*/


class EventChannel<T : IEvent>(private val mBase: IEventChannel<T>) : IEventChannel<T> {


    override fun observeAll(observer: (event: T) -> Unit) {
        mBase.observeAll(observer)
    }

    override fun send(event: T) {
        mBase.send(event)
    }

    fun <E : IEvent> getSpecifiedEventChannel(cls: Class<E>): ISpecifiedEventChannel<E> {

    }
}


inline fun <reified T : IEvent> createEventChannel(/*owner: LifecycleOwner*/): IEventChannel<T> {
    val result: IEventChannel<T>? = null
    return EventChannel(result!!)
}

inline fun <reified E : IEvent> IEventChannel<*>.getSpecifiedEventChannel(): ISpecifiedEventChannel<E> {
    return (this as EventChannel<*>).getSpecifiedEventChannel(E::class.java)
}


class TypedData(data: Any?) {

    private val mData: Any? = data

    @Override
    inline fun <reified T> getTypedData(): T? {
        val data = getData() ?: return null
        return if (data is T) {
            data
        } else {
            // TODO: 2021/4/22 add log
            null
        }
    }

    fun getData(): Any? {
        return mData
    }

}


interface IEvent


sealed class TestListEvent : IEvent {

    object ListEvent1 : TestListEvent()

    data class ListEvent2(val url: String?) : TestListEvent()

}

sealed class TestItemEvent : IEvent {

    object InEvent1 : TestItemEvent()

    data class InEvent2(val url: String?) : TestItemEvent()

}


class Test {


    fun test() {
        val eventChannelFactory = createEventChannel<TestListEvent>()
        eventChannelFactory.observeAll {
            when (it) {
                is TestListEvent.ListEvent1 -> {

                }
                is TestListEvent.ListEvent2 -> {

                }
            }
        }
        val eventChannel = eventChannelFactory.getSpecifiedEventChannel<TestListEvent.ListEvent1>()
        eventChannelFactory.observe(
            TestListEvent.ListEvent1::class.java,
            TestListEvent.ListEvent2::class.java
        ) {

        }
    }
}