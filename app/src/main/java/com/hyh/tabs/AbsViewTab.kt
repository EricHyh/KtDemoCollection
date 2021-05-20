package com.hyh.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * View Tab 基类
 *
 * @author eriche
 * @data 2021/5/20
 */
abstract class AbsViewTab(val parentFragment: Fragment) :
    ITab,
    LifecycleOwner,
    ViewModelStoreOwner {

    companion object {
        private const val TAG = "AbsViewTab"
    }

    private val viewModelStore: ViewModelStore = ViewModelStore()

    private val viewLifecycle: LifecycleRegistry by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleRegistry(this)
    }

    val coroutineScope: CoroutineScope by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LifecycleCoroutineScopeImpl(
            viewLifecycle,
            SupervisorJob() + Dispatchers.Main.immediate
        ).apply {
            register()
        }
    }

    var isVisible: Boolean = false

    var view: View? = null

    override fun getLifecycle(): Lifecycle = viewLifecycle

    override fun getViewModelStore(): ViewModelStore = viewModelStore

    fun performCreate() {
        onCreate()
    }

    fun performCreateView(inflater: LayoutInflater, parent: ViewGroup): View {
        viewLifecycle.currentState = Lifecycle.State.STARTED
        val view = onCreateView(inflater, parent)
        this.view = view
        return view
    }

    fun performViewCreated(view: View) {
        onViewCreated(view)
    }

    fun performTabVisible() {
        viewLifecycle.currentState = Lifecycle.State.RESUMED
        isVisible = true
        onTabVisible()
    }

    fun performTabInvisible() {
        viewLifecycle.currentState = Lifecycle.State.STARTED
        isVisible = false
        onTabInvisible()
    }

    fun performDestroyView() {
        viewLifecycle.currentState = Lifecycle.State.DESTROYED
        onDestroyView()
    }

    fun performDestroy() {
        onDestroy()
    }

    @Suppress
    protected fun onCreate() {
    }

    @Suppress
    protected abstract fun onCreateView(inflater: LayoutInflater, parent: ViewGroup): View

    @Suppress
    protected fun onViewCreated(view: View) {
    }

    @Suppress
    protected fun onTabVisible() {

    }

    @Suppress
    protected fun onTabInvisible() {

    }

    @Suppress
    protected fun onDestroyView() {
    }

    @Suppress
    protected fun onDestroy() {
    }


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