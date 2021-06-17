package com.hyh.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*
import com.hyh.fragment.BaseFragment
import com.hyh.page.PageContext
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * View Tab 基类
 *
 * @author eriche
 * @data 2021/5/20
 */
abstract class AbsViewTab(
    @Suppress("unused")
    val parentPageContext: PageContext
) :
    ITab,
    ViewModelStoreOwner,
    LifecycleOwner {

    companion object {
        private const val TAG = "AbsViewTab"
    }

    private val viewModelStore: ViewModelStore = ViewModelStore()

    private val lifecycle: LifecycleRegistry by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleRegistry(this)
    }

    private val viewLifecycle: LifecycleRegistry by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleRegistry(this)
    }

    private val viewLifecycleOwner: LifecycleOwner by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleOwner {
            viewLifecycle
        }
    }

    @Suppress("unused")
    val coroutineScope: CoroutineScope by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleCoroutineScopeImpl(
            lifecycle, SupervisorJob() + Dispatchers.Main.immediate
        ).apply {
            register()
        }
    }

    @Suppress("unused")
    val pageContext: PageContext by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PageContext.get(this, this)
    }

    var isVisible: Boolean = false

    var view: View? = null

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun getViewModelStore(): ViewModelStore = viewModelStore

    @Suppress("unused")
    fun getViewLifecycle(): Lifecycle = viewLifecycle

    @Suppress("unused")
    @JvmName("_getViewLifecycleOwner")
    fun getViewLifecycleOwner(): LifecycleOwner = viewLifecycleOwner

    internal fun performCreate() {
        onCreate()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    internal fun performCreateView(inflater: LayoutInflater, parent: ViewGroup): View {
        val view = onCreateView(inflater, parent)
        this.view = view
        viewLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        return view
    }

    internal fun performViewCreated(view: View) {
        onViewCreated(view)
    }

    internal fun performTabVisible() {
        viewLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        isVisible = true
        onTabVisible()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        viewLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    internal fun performTabInvisible() {
        viewLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        isVisible = false
        onTabInvisible()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        viewLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    internal fun performDestroyView() {

        if (view != null) {
            viewLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        onDestroyView()
    }

    internal fun performDestroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        onDestroy()
    }

    protected open fun onCreate() {}

    protected abstract fun onCreateView(inflater: LayoutInflater, parent: ViewGroup): View

    protected open fun onViewCreated(view: View) {}

    protected open fun onTabVisible() {}

    protected open fun onTabInvisible() {}

    protected open fun onDestroyView() {}

    protected open fun onDestroy() {}

    class LifecycleCoroutineScopeImpl(
        private val lifecycle: Lifecycle,
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
}