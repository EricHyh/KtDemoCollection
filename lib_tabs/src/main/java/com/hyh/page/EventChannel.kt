package com.hyh.page

import android.util.Log
import androidx.lifecycle.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass


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
    fun <T : IEvent> getObservable(eventType: KClass<T>): Observable<T> = getObservable(eventType.java)

    fun getObservable(vararg eventTypes: Class<out IEvent>): Observable<IEvent>
    fun getObservable(vararg eventTypes: KClass<out IEvent>): Observable<IEvent> = getObservable(*eventTypes.map { it.java }.toTypedArray())

    fun getObservable(): Observable<IEvent>

    // endregion

    // region receive as flow

    fun <T : IEvent> getFlow(eventType: Class<T>): Flow<T>
    fun <T : IEvent> getFlow(eventType: KClass<T>): Flow<T> = getFlow(eventType.java)

    fun getFlow(vararg eventTypes: Class<out IEvent>): Flow<IEvent>
    fun getFlow(vararg eventTypes: KClass<out IEvent>): Flow<IEvent> = getFlow(*eventTypes.map { it.java }.toTypedArray())

    fun getFlow(): Flow<IEvent>

    // endregion

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

    override fun send(event: IEvent) {
        eventSource.onNext(event)
        lifecycleScope.launch {
            eventFlow.emit(value = event)
        }
    }

    override fun <T : IEvent> getObservable(eventType: Class<T>): Observable<T> {
        return eventSource.ofType(eventType)
    }

    override fun getObservable(vararg eventTypes: Class<out IEvent>): Observable<IEvent> {
        return eventSource.filter {
            it.isInstanceOf(*eventTypes)
        }
    }

    override fun getObservable(): Observable<IEvent> {
        return eventSource
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : IEvent> getFlow(eventType: Class<T>): Flow<T> {
        return eventFlow.asSharedFlow().filter {
            eventType.isInstance(it)
        }.map {
            it as T
        }
    }

    override fun getFlow(vararg eventTypes: Class<out IEvent>): Flow<IEvent> {
        return eventFlow.asSharedFlow().filter {
            it.isInstanceOf(*eventTypes)
        }
    }

    override fun getFlow(): Flow<IEvent> {
        return eventFlow.asSharedFlow()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        eventSource.onComplete()
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


sealed class Inner : IEvent {

    data class First(val num: Int) : Inner()
    data class Second(val num: Int) : Inner()

}

sealed class Outer : IEvent {
    data class First(val num: Int) : Outer()
    data class Second(val num: Int) : Outer()
}


fun text(eventChannel: IEventChannel, storage: IStorage) {

    GlobalScope.launch {

        eventChannel
            .getFlow(ItemEvent::class)
            .collect {
                when (it) {
                    is ItemEvent.ItemClick -> {
                        val itemData = it.itemData
                    }
                    is ItemEvent.ItemRemove -> {
                        val itemData = it.itemData
                    }
                }
            }


        eventChannel
            .getFlow(ListEvent::class)
            .collect {
                when (it) {
                    is ListEvent.Scroll -> {
                        val offset = it.offset
                    }
                    is ListEvent.ScrollStateChanged -> {
                        val state = it.state
                    }
                }
            }
    }


    storage.store(HistoryOrderStore.SelectedAccountId(100L))

    val selectedAccountId: Long? = storage.get(HistoryOrderStore.SelectedAccountId::class)

    storage.observeForever(HistoryOrderStore.SelectedAccountId::class) {
        val accountId = it
    }

}


sealed class ItemEvent : IEvent {
    data class ItemClick(val itemData: Any) : ItemEvent()
    data class ItemRemove(val itemData: Any) : ItemEvent()
}

sealed class ListEvent : IEvent {
    data class Scroll(val offset: Float) : ListEvent()
    data class ScrollStateChanged(val state: Int) : ListEvent()
}


sealed class HistoryOrderStore<V> : IStore<V> {

    data class SelectedAccountId(override val value: Long) : HistoryOrderStore<Long>()

}



