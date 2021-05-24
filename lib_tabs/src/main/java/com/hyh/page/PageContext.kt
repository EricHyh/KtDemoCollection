package com.hyh.page

import androidx.fragment.app.Fragment
import androidx.lifecycle.*

/**
 * 页面上下文
 *
 * @author eriche
 * @data 2021/4/28
 */

class PageContext(
    @Suppress("unused")
    val viewModelStoreOwner: ViewModelStoreOwner,
    @Suppress
    val lifecycleOwner: LifecycleOwner
) {

    companion object {
        fun get(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner
        ): PageContext {
            val viewModel = ViewModelProvider(viewModelStoreOwner).get(PageContextViewModel::class.java)
            return viewModel.getPageContext(viewModelStoreOwner, lifecycleOwner)
        }
    }

    val eventChannel: IEventChannel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IEventChannel.Factory.create(lifecycleOwner)
    }

    val storage: IStorage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IStorage.Factory.create(lifecycleOwner)
    }
}

class PageContextViewModel : ViewModel() {

    private val pageContextMap: MutableMap<Lifecycle, PageContext> = mutableMapOf()

    fun getPageContext(
        viewModelStoreOwner: ViewModelStoreOwner,
        lifecycleOwner: LifecycleOwner
    ): PageContext {
        val pageContext = pageContextMap[lifecycleOwner.lifecycle]
        if (pageContext != null) {
            return pageContext
        }
        synchronized(pageContextMap) {
            return PageContext(viewModelStoreOwner, lifecycleOwner).apply {
                pageContextMap[lifecycleOwner.lifecycle] = this
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
    get() = PageContext.get(this, this)


