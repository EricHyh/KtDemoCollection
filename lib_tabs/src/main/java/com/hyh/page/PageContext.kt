package com.hyh.page

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.hyh.Invoke
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

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

    private val lifecycleInvokeMap: MutableMap<Lifecycle.Event, MutableList<Invoke>> = mutableMapOf()

    init {
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            lifecycleOwner.lifecycle.addObserver(InnerLifecycleEventObserver())
        }
    }

    val lifecycleScope: CoroutineScope by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleCoroutineScopeImpl(lifecycleOwner.lifecycle, SupervisorJob() + Dispatchers.Main.immediate).apply {
            register()
        }
    }

    val eventChannel: IEventChannel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IEventChannel.Factory.create(lifecycleOwner.lifecycle, lifecycleScope)
    }

    val storage: IStorage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IStorage.Factory.create(lifecycleOwner.lifecycle)
    }

    fun invokeOnDestroy(block: () -> Unit) {
        runOnMainThread {
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                var mutableList: MutableList<Invoke>? = lifecycleInvokeMap[Lifecycle.Event.ON_DESTROY]
                if (mutableList == null) {
                    mutableList = mutableListOf()
                    mutableList.add(block)
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

private class PageContextViewModel : ViewModel() {

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


val AppCompatActivity.pageContext: PageContext
    get() = PageContext.get(this, this)