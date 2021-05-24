package com.hyh.event

import androidx.lifecycle.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.CoroutineContext


interface IEvent {
    companion object {
        fun Any.asEvent(): IEvent {
            return DataWrapperEvent.create(this)
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

class EventChannel private constructor(owner: LifecycleOwner) : IEventChannel, LifecycleObserver {

    companion object {

        fun create(owner: LifecycleOwner): IEventChannel {
            return EventChannel(owner)
        }

        @Suppress("UNCHECKED_CAST")
        fun IEventChannel.getDataWrapperEventObservable(): Observable<IEvent> {
            return getObservable(DataWrapperEvent::class.java) as Observable<IEvent>
        }
    }

    private val lifecycleOwner = owner

    private val eventSource = PublishSubject.create<IEvent>()

    private val eventFlow = MutableSharedFlow<IEvent>()

    private val lifecycleScope: CoroutineScope by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        FtLifecycleCoroutineScopeImpl(owner.lifecycle, SupervisorJob() + Dispatchers.Main.immediate).apply {
            register()
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        eventSource.onComplete()
    }

    override fun send(event: IEvent) {
        eventSource.onNext(event)
        lifecycleScope.launch {
            eventFlow.emit(value = event)
        }
    }

    override fun <T : IEvent> getObservable(eventType: Class<T>): Observable<T> {
        return eventSource.ofType(eventType)
    }

    override fun getObservable(vararg eventTypes: Class<*>): Observable<IEvent> {
        return eventSource.filter {
            it.isInstanceOf(*eventTypes)
        }
    }

    override fun getObservable(): Observable<IEvent> {
        return eventSource
    }

    override fun getFlow(): Flow<IEvent> {
        return eventFlow.asSharedFlow()
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

    class FtLifecycleCoroutineScopeImpl(
        val lifecycle: Lifecycle,
        override val coroutineContext: CoroutineContext
    ) : CoroutineScope, LifecycleEventObserver {

        init {
            // in case we are initialized on a non-main thread, make a best effort check before
            // we return the scope. This is not sync but if developer is launching on a non-main
            // dispatcher, they cannot be 100% sure anyways.
            if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
                coroutineContext.cancel()
            }
        }

        fun register() {
            launch(Dispatchers.Main.immediate) {
                if (lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
                    lifecycle.addObserver(this@FtLifecycleCoroutineScopeImpl)
                } else {
                    coroutineContext.cancel()
                }
            }
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (lifecycle.currentState <= Lifecycle.State.DESTROYED) {
                lifecycle.removeObserver(this)
                coroutineContext.cancel()
            }
        }
    }
}

object EventLifecycleHelper {

    fun <T> Observable<T>.bindToLifecycle(owner: LifecycleOwner): Observable<T> {


        return this
    }

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

