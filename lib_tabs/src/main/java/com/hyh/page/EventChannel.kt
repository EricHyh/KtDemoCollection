package com.hyh.page

import androidx.lifecycle.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass


/**
 * 事件数据接口，被发射的事件需要实现该接口
 *
 * @author eriche
 * @data 2021/6/18
 */
interface IEvent {
    companion object {

        /**
         * 将任意对象包装成事件数据
         *
         * @return
         */
        fun Any.asEvent(): IEvent {
            return DataWrapperEvent.create(this)
        }

        inline fun <reified T : IEvent> IEventChannel.getObservable(): Observable<T> {
            return getObservable(T::class.java)
        }


        /**
         * 是否为指定数据类型的包装事件
         *
         * @param T 被包装的数据类型
         * @return
         */
        inline fun <reified T> IEvent.isDataWrapperEvent(): Boolean {
            if (this !is DataWrapperEvent) {
                return false
            }
            val data = this.getData()
            return T::class.java.isInstance(data)
        }

        /**
         * 将包装的数据解包出来
         *
         * @param T 被包装的数据类型
         * @return
         */
        inline fun <reified T> IEvent.unwrapData(): T? {
            if (isDataWrapperEvent<T>()) {
                return (this as DataWrapperEvent).getData() as T
            }
            return null
        }
    }
}

/**
 * 包装数据事件类型
 *
 * @property data
 */
class DataWrapperEvent private constructor(private val data: Any) : IEvent {

    companion object {
        fun create(data: Any): DataWrapperEvent {
            return DataWrapperEvent(data)
        }
    }

    fun getData(): Any {
        return data
    }
}

/**
 * 提供给页面使用的事件通道，会绑定页面生命周期，在页面destroy后停止发射数据
 */
interface IEventChannel {

    object Factory {
        fun create(lifecycle: Lifecycle, lifecycleScope: CoroutineScope): IEventChannel {
            return EventChannel.create(lifecycle, lifecycleScope)
        }
    }

    // region receive as observable

    /**
     * 提供一个[Observable]对象，该对象可以观察[T]类型及其子类型的事件.
     *
     * 注意：不使用时请注销观察者
     *
     *
     * @param T 事件类型
     * @param eventType 事件类型的java字节码对象
     * @return
     */
    fun <T : IEvent> getObservable(eventType: Class<T>): Observable<T>
    fun <T : IEvent> getObservable(eventType: KClass<T>): Observable<T> = getObservable(eventType.java)

    /**
     * 提供一个[Observable]对象，该对象可以观察[eventTypes]中每一项的类型及其子类型的事件
     *
     * 注意：不使用时请注销观察者
     *
     * @param eventTypes 事件类型的java字节码对象列表
     * @return
     */
    fun getObservable(vararg eventTypes: Class<out IEvent>): Observable<IEvent>
    fun getObservable(vararg eventTypes: KClass<out IEvent>): Observable<IEvent> = getObservable(*eventTypes.map { it.java }.toTypedArray())

    /**
     * 提供一个[Observable]对象，该对象可以观察事件通道中的所有事件
     *
     * 注意：不使用时请注销观察者
     *
     * @return
     */
    fun getObservable(): Observable<IEvent>

    // endregion

    // region receive as flow

    /**
     * 提供一个[Flow]对象，该对象可以收集[T]类型及其子类型的事件
     *
     * 注意：不使用时请销毁协程任务
     *
     * @param T 事件类型
     * @param eventType 事件类型的java字节码对象
     * @return
     */
    fun <T : IEvent> getFlow(eventType: Class<T>): Flow<T>
    fun <T : IEvent> getFlow(eventType: KClass<T>): Flow<T> = getFlow(eventType.java)

    /**
     * 提供一个[Flow]对象，该对象可以收集[eventTypes]中每一项的类型及其子类型的事件
     *
     * 注意：不使用时请销毁协程任务
     *
     * @param eventTypes 事件类型的java字节码对象列表
     * @return
     */
    fun getFlow(vararg eventTypes: Class<out IEvent>): Flow<IEvent>
    fun getFlow(vararg eventTypes: KClass<out IEvent>): Flow<IEvent> = getFlow(*eventTypes.map { it.java }.toTypedArray())

    /**
     * 提供一个[Flow]对象，使用该对象可以收集该事件通道中的所有事件
     *
     * 注意：不使用时请销毁协程任务
     *
     * @return
     */
    fun getFlow(): Flow<IEvent>

    // endregion

    /**
     * 发射事件
     *
     * @param event 事件类型
     */
    fun send(event: IEvent)
}

class EventChannel private constructor(
    private val lifecycle: Lifecycle,
    private val lifecycleScope: CoroutineScope
) : IEventChannel, LifecycleObserver {

    companion object {

        fun create(lifecycle: Lifecycle, lifecycleScope: CoroutineScope): IEventChannel {
            return EventChannel(lifecycle, lifecycleScope)
        }

        @Suppress("UNCHECKED_CAST")
        fun IEventChannel.getDataWrapperEventObservable(): Observable<IEvent> {
            return getObservable(DataWrapperEvent::class.java) as Observable<IEvent>
        }
    }

    private val eventSource = PublishSubject.create<IEvent>()

    private val eventFlow = MutableSharedFlow<IEvent>()


    init {
        lifecycle.addObserver(this)
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



