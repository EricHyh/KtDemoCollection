package com.hyh.page.state

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hyh.page.IStore
import com.hyh.page.PageContext

class PageStateController(private val pageContext: PageContext) {


    companion object {

        private const val TAG = "PageStateController"

        private val handler: Handler by lazy {
            Handler(Looper.getMainLooper())
        }

        fun get(pageContext: PageContext): PageStateController {
            val controller = pageContext.storage.get(PageStateControllerStore::class)
            if (controller != null) {
                return controller
            }
            return PageStateController(pageContext).apply {
                pageContext.storage.store(PageStateControllerStore(this))
            }
        }
    }

    init {
        pageContext.invokeOnDestroy {
            states.forEach {
                it.removeObserve(unitStateObserver)
            }
            states.clear()
        }
    }

    private val _pageStateLiveData = MutableLiveData(PageState.LOADING)
    val pageStateLiveData: LiveData<PageState>
        get() = _pageStateLiveData

    private val unitStateObserver = UnitStateObserver()

    private val states: MutableSet<IPageUnitState> = HashSet()

    private var pageStateStrategy: IPageStateStrategy = OneSuccessStrategy()

    @Synchronized
    fun setPageStateStrategy(strategy: IPageStateStrategy) {
        if (!checkLifecycle()) return
        this.pageStateStrategy = strategy
        refreshState()
    }

    @Synchronized
    fun refresh() {
        if (!checkLifecycle()) return
        if (_pageStateLiveData.value != PageState.LOADING) {
            _pageStateLiveData.value = PageState.LOADING
        }
        states.forEach {
            it.forceRefreshState()
        }
        refreshState()
    }

    @Synchronized
    fun addPageUnitState(unitState: IPageUnitState) {
        if (!checkLifecycle() || states.contains(unitState)) return
        states.add(unitState)
        unitState.observe(unitStateObserver)
        refreshState()
    }

    @Synchronized
    fun removePageUnitState(unitState: IPageUnitState) {
        if (!checkLifecycle()) return
        unitState.removeObserve(unitStateObserver)
        states.remove(unitState)
        refreshState()
    }

    private fun refreshState() {
        if (!checkLifecycle()) return
        handler.post {
            if (checkLifecycle()) {
                val states = states.map { it.getState() }
                val newPageState = pageStateStrategy.calculatePageState(states)
                if (_pageStateLiveData.value != newPageState) {
                    _pageStateLiveData.value = newPageState
                }
            }
        }
    }

    private fun checkLifecycle(): Boolean {
        if (pageContext.lifecycle.currentState != Lifecycle.State.DESTROYED) return true
        return false
    }

    inner class UnitStateObserver : Observer<Pair<IPageUnitState, UnitState>> {

        override fun onChanged(pair: Pair<IPageUnitState, UnitState>?) {
            refreshState()
        }
    }

    class PageStateControllerStore(override val value: PageStateController) :
        IStore<PageStateController>
}