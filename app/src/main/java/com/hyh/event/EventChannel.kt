package com.hyh.event

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleOwner
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.CoroutineContext


interface IEvent {
    fun Any.asEvent(): IEvent {
        return DataWrapperEvent.create(this)
    }
}

class DataWrapperEvent private constructor(private val mData: Any) : IEvent {

    companion object {
        fun create(data: Any): DataWrapperEvent {
            return DataWrapperEvent(data)
        }
    }

    fun getData(): Any {
        return mData
    }
}

interface IEventChannel {

    object Factory {
        fun create(owner: LifecycleOwner): IEventChannel {
            return EventChannel.create(owner)
        }
    }

    // region receive as observable

    fun <T : IEvent> getObservable(eventType: Class<T>): Observable<T>
    fun getObservable(vararg eventTypes: Class<*>): Observable<IEvent>
    fun getObservable(): Observable<IEvent>

    // endregion

    fun getFlow(): Flow<IEvent>


    fun send(event: IEvent)
}

class EventChannel private constructor(owner: LifecycleOwner) : IEventChannel {

    companion object {
        fun create(owner: LifecycleOwner): IEventChannel {
            return EventChannel(owner)
        }
    }

    private val mLifecycleOwner = owner

    private val mEventSource = PublishSubject.create<IEvent>()

    private val mEventFlow = MutableSharedFlow<IEvent>()

    override fun send(event: IEvent) {
        mEventSource.onNext(event)
        mEventFlow.tryEmit(value = event)
    }

    override fun <T : IEvent> getObservable(eventType: Class<T>): Observable<T> {
        return mEventSource.ofType(eventType)
    }


    override fun getObservable(vararg eventTypes: Class<*>): Observable<IEvent> {
        return mEventSource.filter {
            it.isInstanceOf(*eventTypes)
        }
    }

    override fun getObservable(): Observable<IEvent> {
        return mEventSource
    }

    override fun getFlow(): Flow<IEvent> {
        return mEventFlow.asSharedFlow()
    }

    private fun IEvent.isInstanceOf(vararg eventTypes: Class<*>): Boolean {
        if (eventTypes.isEmpty()) return false
        eventTypes.forEach {
            if (it.isInstance(this)) {
                return true
            }
        }
        return false
    }
}

@Suppress("UNCHECKED_CAST")
fun IEventChannel.getDataWrapperEventObservable(): Observable<IEvent> {
    return getObservable(DataWrapperEvent::class.java) as Observable<IEvent>
}

inline fun <reified T : IEvent> IEventChannel.getObservable(): Observable<T> {
    return getObservable(T::class.java)
}

inline fun <reified T> IEvent.isDataWrapperEvent(): Boolean {
    if (this !is DataWrapperEvent) {
        return false
    }
    val data = this.getData()
    return T::class.java.isInstance(data)
}

inline fun <reified T> IEvent.unwrapData(): T? {
    if (isDataWrapperEvent<T>()) {
        return (this as DataWrapperEvent).getData() as T
    }
    return null
}

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

    }
}