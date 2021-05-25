package com.hyh.page

import androidx.lifecycle.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

interface IStorage {

    object Factory {
        fun create(owner: LifecycleOwner): IStorage {
            return StorageImpl.create(owner)
        }
    }

    fun store(store: IStore<*>)

    fun postStore(store: IStore<*>)

    fun <Value> get(cls: Class<out IStore<Value>>): Value?
    fun <Value> get(cls: KClass<out IStore<Value>>): Value? = get(cls.java)

    fun <Value> observeForever(cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit): Observer<Value>?
    fun <Value> observeForever(cls: KClass<out IStore<Value>>, onChanged: (value: Value) -> Unit): Observer<Value>? =
        observeForever(cls.java, onChanged)

    fun <Value> observeForever(cls: Class<out IStore<Value>>, observer: Observer<Value>)
    fun <Value> observeForever(cls: KClass<out IStore<Value>>, observer: Observer<Value>) =
        observeForever(cls.java, observer)

    fun <Value> observe(owner: LifecycleOwner, cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit): Observer<Value>?
    fun <Value> observe(owner: LifecycleOwner, cls: KClass<out IStore<Value>>, onChanged: (value: Value) -> Unit): Observer<Value>? =
        observe(owner, cls.java, onChanged)

    fun <Value> observe(owner: LifecycleOwner, cls: Class<out IStore<Value>>, observer: Observer<Value>)
    fun <Value> observe(owner: LifecycleOwner, cls: KClass<out IStore<Value>>, observer: Observer<Value>) =
        observe(owner, cls.java, observer)

    fun <Value> removeObserver(cls: Class<out IStore<Value>>, observer: Observer<Value>)
    fun <Value> removeObserver(cls: KClass<out IStore<Value>>, observer: Observer<Value>) =
        removeObserver(cls.java, observer)
}


class StorageImpl private constructor(private val owner: LifecycleOwner) : IStorage, LifecycleObserver {

    companion object {
        fun create(owner: LifecycleOwner): IStorage {
            return StorageImpl(owner)
        }
    }

    private val isBoundLifeCycle: AtomicBoolean = AtomicBoolean(false)
    private val storeMap: MutableMap<Class<out IStore<*>>, MutableLiveData<Any?>> = mutableMapOf()

    override fun store(store: IStore<*>) {
        bindLifeCycle()
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val value = store.value
        val cls: Class<out IStore<*>> = store::class.java
        val mutableLiveData = storeMap[cls]
        if (mutableLiveData != null) {
            mutableLiveData.value = value
            return
        }
        prepareLiveData(cls = cls, value = value)
    }

    override fun postStore(store: IStore<*>) {
        bindLifeCycle()
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val value = store.value
        val cls: Class<out IStore<*>> = store::class.java
        val mutableLiveData = storeMap[cls]
        if (mutableLiveData != null) {
            mutableLiveData.postValue(value)
            return
        }
        prepareLiveData(cls = cls, value = value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Value> get(cls: Class<out IStore<Value>>): Value? {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return null
        val mutableLiveData = storeMap[cls] ?: return null
        return mutableLiveData.value as? Value
    }

    override fun <Value> observeForever(cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit): Observer<Value>? {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return null
        val liveData = prepareLiveData(cls)
        val wrappedObserver = Observer<Value> { value -> onChanged.invoke(value) }
        liveData.observeForever(wrappedObserver)
        return wrappedObserver
    }

    override fun <Value> observeForever(cls: Class<out IStore<Value>>, observer: Observer<Value>) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val liveData = prepareLiveData(cls)
        liveData.observeForever(observer)
    }

    override fun <Value> observe(owner: LifecycleOwner, cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit): Observer<Value>? {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return null
        val liveData = prepareLiveData(cls)
        return liveData.observe(owner, onChanged)
    }

    override fun <Value> observe(owner: LifecycleOwner, cls: Class<out IStore<Value>>, observer: Observer<Value>) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val liveData = prepareLiveData(cls)
        liveData.observe(owner, observer)
    }

    override fun <Value> removeObserver(cls: Class<out IStore<Value>>, observer: Observer<Value>) {
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val liveData = getTypedLiveData(cls) ?: return
        liveData.removeObserver(observer)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        synchronized(storeMap) {
            storeMap.clear()
        }
    }

    private fun <Value> prepareLiveData(cls: Class<out IStore<Value>>): MutableLiveData<Value> {
        val mutableLiveData = getTypedLiveData(cls)
        if (mutableLiveData != null) return mutableLiveData
        synchronized(storeMap) {
            var newMutableLiveData = getTypedLiveData(cls)
            if (newMutableLiveData == null) {
                newMutableLiveData = MutableLiveData<Value>().apply {
                    storeMap[cls] = this.asAnyLiveData()
                }
            }
            return newMutableLiveData
        }
    }

    private fun prepareLiveData(cls: Class<out IStore<*>>, value: Any?): MutableLiveData<Any?> {
        val mutableLiveData = storeMap[cls]
        if (mutableLiveData != null) return mutableLiveData
        synchronized(storeMap) {
            var newMutableLiveData = storeMap[cls]
            if (newMutableLiveData == null) {
                newMutableLiveData = MutableLiveData(value).apply {
                    storeMap[cls] = this
                }
            }
            return newMutableLiveData
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Value> MutableLiveData<Value>.asAnyLiveData(): MutableLiveData<Any?> {
        return this as MutableLiveData<Any?>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Value> getTypedLiveData(cls: Class<out IStore<Value>>): MutableLiveData<Value>? {
        val mutableLiveData = storeMap[cls] ?: return null
        return mutableLiveData as MutableLiveData<Value>
    }

    private fun bindLifeCycle() {
        if (isBoundLifeCycle.get()) return
        synchronized(isBoundLifeCycle) {
            if (isBoundLifeCycle.get()) return
            isBoundLifeCycle.set(true)
            owner.lifecycle.addObserver(this)
        }
    }
}


interface IStore<Value> {
    val value: Value
}