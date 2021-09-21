package com.hyh.page.state

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hyh.page.IStore
import com.hyh.page.PageContext

class PageStateController(private val pageContext: PageContext) {

    companion object {
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

    fun setPageStateStrategy(strategy: IPageStateStrategy) {
        if (!checkLifecycle()) return
        this.pageStateStrategy = strategy
        refreshState()
    }

    fun addPageUnitState(unitState: IPageUnitState) {
        if (!checkLifecycle() || states.contains(unitState)) return
        states.add(unitState)
        unitState.observe(unitStateObserver)
        refreshState()
    }

    fun removePageUnitState(unitState: IPageUnitState) {
        if (!checkLifecycle()) return
        unitState.removeObserve(unitStateObserver)
        states.remove(unitState)
        refreshState()
    }

    private fun refreshState() {
        if (!checkLifecycle()) return
        val states = states.map { it.getState() }
        val newPageState = pageStateStrategy.calculatePageState(states)
        if (_pageStateLiveData.value == newPageState) return
        _pageStateLiveData.postValue(newPageState)
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