package com.hyh.page

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.hyh.Invoke
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * 页面上下文
 *
 * @author eriche
 * @data 2021/4/28
 */
abstract class PageContext(
    @Suppress("unused")
    val viewModelStoreOwner: ViewModelStoreOwner,
    @Suppress
    val lifecycleOwner: LifecycleOwner
) {

    companion object {

        val lock = Any()

        fun get(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner
        ): PageContext {
            if (viewModelStoreOwner is Activity) {
                if (viewModelStoreOwner.application == null) {
                    return LazyPageContext(viewModelStoreOwner, lifecycleOwner)
                }
            } else if (viewModelStoreOwner is Fragment) {
                if (viewModelStoreOwner.fragmentManager == null) {
                    return LazyPageContext(viewModelStoreOwner, lifecycleOwner)
                }
            }
            val result = viewModelStoreOwner.runCatching {
                ViewModelProvider(viewModelStoreOwner).get(PageContextViewModel::class.java)
            }
            val viewModel = result.getOrNull()
            return if (viewModel != null) {
                val cachePageContext = LazyPageContext.getCachePageContext(lifecycleOwner)
                if (cachePageContext != null) {
                    viewModel.setPageContext(lifecycleOwner.lifecycle, cachePageContext)
                    cachePageContext
                } else {
                    viewModel.getPageContext(viewModelStoreOwner, lifecycleOwner)
                }
            } else {
                LazyPageContext(viewModelStoreOwner, lifecycleOwner)
            }
        }
    }

    abstract val lifecycle: Lifecycle
    abstract val lifecycleScope: CoroutineScope
    abstract val eventChannel: IEventChannel
    abstract val storage: IStorage
    abstract fun invokeOnDestroy(block: () -> Unit)

    /**
     * 创建一个局部的页面上下文，该上下文拥有独立的事件通道和数据存储器.
     *
     * 当一个页面过于复杂时，可能这个页面分成很多区域，并且这些区域没有关联性，那么可以针对这些区域生成一个局部的上下文
     *
     * @return
     */
    fun createLocalPageContext(): PageContext {
        return PageContextImpl(viewModelStoreOwner, lifecycleOwner)
    }
}


class LazyPageContext(
    viewModelStoreOwner: ViewModelStoreOwner,
    lifecycleOwner: LifecycleOwner
) : PageContext(viewModelStoreOwner, lifecycleOwner) {

    companion object {

        private val pageContextMap: MutableMap<Int, WeakReference<out PageContext>> = mutableMapOf()

        fun getCachePageContext(lifecycleOwner: LifecycleOwner): PageContext? {
            synchronized(lock) {
                val hashCode = System.identityHashCode(lifecycleOwner.lifecycle)
                return pageContextMap[hashCode]?.get()
            }
        }

        private fun obtain(viewModelStoreOwner: ViewModelStoreOwner, lifecycleOwner: LifecycleOwner): PageContext {
            synchronized(lock) {
                val iterator = pageContextMap.entries.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (next.value.get() == null) {
                        iterator.remove()
                    }
                }
                val hashCode = System.identityHashCode(lifecycleOwner.lifecycle)
                val pageContextRef = pageContextMap[hashCode]
                var pageContext = pageContextRef?.get()
                if (pageContext != null) return pageContext
                pageContext = PageContextImpl(viewModelStoreOwner, lifecycleOwner)
                pageContextMap[hashCode] = WeakReference(pageContext)
                return pageContext
            }
        }
    }

    private val realPageContext by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        obtain(viewModelStoreOwner, lifecycleOwner).apply {
            invokeOnDestroy {
                synchronized(lock) {
                    val hashCode = System.identityHashCode(lifecycleOwner.lifecycle)
                    val pageContextRef = pageContextMap.remove(hashCode)
                    pageContextRef?.clear()
                }
            }
        }
    }
    override val lifecycle: Lifecycle
        get() = realPageContext.lifecycle
    override val lifecycleScope: CoroutineScope
        get() = realPageContext.lifecycleScope
    override val eventChannel: IEventChannel
        get() = realPageContext.eventChannel
    override val storage: IStorage
        get() = realPageContext.storage

    override fun invokeOnDestroy(block: () -> Unit) {
        realPageContext.invokeOnDestroy(block)
    }
}


class PageContextImpl(
    viewModelStoreOwner: ViewModelStoreOwner,
    lifecycleOwner: LifecycleOwner
) : PageContext(viewModelStoreOwner, lifecycleOwner) {


    companion object {
        fun get(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner
        ): PageContext {
            val viewModel = ViewModelProvider(viewModelStoreOwner).get(PageContextViewModel::class.java)
            return viewModel.getPageContext(viewModelStoreOwner, lifecycleOwner)
        }
    }

    private val lifecycleInvokeMap: MutableMap<Lifecycle.Event, MutableList<Invoke>> = mutableMapOf()

    init {
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            lifecycleOwner.lifecycle.addObserver(InnerLifecycleEventObserver())
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    override val lifecycleScope: CoroutineScope by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleCoroutineScopeImpl(lifecycleOwner.lifecycle, SupervisorJob() + Dispatchers.Main.immediate).apply {
            register()
        }
    }

    override val eventChannel: IEventChannel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IEventChannel.Factory.create(lifecycleOwner.lifecycle, lifecycleScope)
    }

    override val storage: IStorage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IStorage.Factory.create(lifecycleOwner.lifecycle)
    }

    override fun invokeOnDestroy(block: () -> Unit) {
        runOnMainThread {
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                var mutableList: MutableList<Invoke>? = lifecycleInvokeMap[Lifecycle.Event.ON_DESTROY]
                if (mutableList == null) {
                    mutableList = mutableListOf()
                    mutableList.add(block)
                    lifecycleInvokeMap[Lifecycle.Event.ON_DESTROY] = mutableList
                } else {
                    mutableList.add(block)
                }
            }
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Thread.currentThread() == Looper.getMainLooper()?.thread) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }

    inner class InnerLifecycleEventObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val mutableList = lifecycleInvokeMap[event] ?: return
            mutableList.forEach {
                it()
            }
        }
    }
}

private class LifecycleCoroutineScopeImpl(
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
                lifecycle.addObserver(this@LifecycleCoroutineScopeImpl)
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
        synchronized(PageContext.lock) {
            return PageContextImpl(viewModelStoreOwner, lifecycleOwner).apply {
                pageContextMap[lifecycleOwner.lifecycle] = this
            }
        }
    }

    fun setPageContext(lifecycle: Lifecycle, pageContext: PageContext) {
        synchronized(PageContext.lock) {
            pageContextMap[lifecycle] = pageContext
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(PageContext.lock) {
            pageContextMap.clear()
        }
    }
}

val Fragment.pageContext: PageContext
    get() = PageContext.get(this, this)


val AppCompatActivity.pageContext: PageContext
    get() = PageContext.get(this, this)