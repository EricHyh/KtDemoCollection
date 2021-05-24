package com.hyh.event

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.hyh.event.PageContext.Companion.getPageContext
import io.reactivex.Observable

/**
 * 页面上下文
 *
 * @author eriche
 * @data 2021/4/28
 */
class PageContext(owner: LifecycleOwner) {

    companion object {
        fun ViewModelStoreOwner.getPageContext(owner: LifecycleOwner): PageContext {
            val viewModel = ViewModelProvider(this).get(PageContextViewModel::class.java)
            return viewModel.getPageContext(owner)
        }
    }

    val eventChannel: IEventChannel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IEventChannel.Factory.create(owner)
    }

    val storage: IStorage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IStorage.Factory.create(owner)
    }

    /*val storage: IStorage = object : IStorage {
        override fun store(any: Any) {
            map[any.javaClass] = any
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(cls: Class<T>): T? {
            val any = map[cls] ?: return null
            if (cls.isInstance(any)) {
                return any as T
            }
            return null
        }
    }

    private val map = mutableMapOf<Type, Any>()*/
}


interface IStorage {

    object Factory {
        fun create(owner: LifecycleOwner): IStorage {
            return StorageImpl.create(owner)
        }
    }

    fun store(store: IStore<*>)

    fun postStore(store: IStore<*>)

    fun <Value> get(cls: Class<out IStore<Value>>): Value?

    fun <Value> observeForever(cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit)

    fun <Value> observe(owner: LifecycleOwner, cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit)

}


class StorageImpl private constructor(owner: LifecycleOwner) : IStorage {

    companion object {
        fun create(owner: LifecycleOwner): IStorage {
            return StorageImpl(owner)
        }
    }

    private val storeMap: MutableMap<Class<out IStore<*>>, MutableLiveData<Any?>> = mutableMapOf()

    override fun store(store: IStore<*>) {
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
        val mutableLiveData = storeMap[cls] ?: return null
        return mutableLiveData.value as? Value
    }

    override fun <Value> observeForever(cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit) {
        val liveData = prepareLiveData(cls)
        liveData.observeForever {
            onChanged(it)
        }
    }

    override fun <Value> observe(owner: LifecycleOwner, cls: Class<out IStore<Value>>, onChanged: (value: Value) -> Unit) {
        val liveData = prepareLiveData(cls)
        liveData.observe(owner, onChanged)
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

}


interface IStore<Value> {
    val value: Value
}


sealed class TestStore<Value> : IStore<Value> {

    data class God<Int>(override val value: Int) : TestStore<Int>()

}


class PageContextViewModel : ViewModel() {

    private val pageContextMap: MutableMap<Lifecycle, PageContext> = mutableMapOf()

    fun getPageContext(owner: LifecycleOwner): PageContext {
        val pageContext = pageContextMap[owner.lifecycle]
        if (pageContext != null) {
            return pageContext
        }
        synchronized(pageContextMap) {
            return PageContext(owner).apply {
                pageContextMap[owner.lifecycle] = this
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(pageContextMap) {
            pageContextMap.clear()
        }
    }
}

val Fragment.pageContext: PageContext
    get() = this.getPageContext(this)