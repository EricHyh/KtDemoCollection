package com.hyh.event

import androidx.lifecycle.*
import java.lang.reflect.Type

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

    private val mMap = mutableMapOf<Type, Any>()
    private val mEventChannel: IEventChannel by lazy {
        IEventChannel.Factory.create(owner)
    }

    fun registerService(service: Any) {
        mMap[service.javaClass] = service
    }

    fun <T> getService(cls: Class<T>): T? {
        val any = mMap[cls] ?: return null
        if (cls.isInstance(any)) {
            return cls as T
        }
        return null
    }

    inline fun <reified T> getService(): T? {
        val cls = T::class.java
        return getService(cls)
    }

    fun getEventChannel(): IEventChannel {
        return mEventChannel
    }
}

private class PageContextViewModel() : ViewModel() {

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