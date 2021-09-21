package com.hyh.paging3demo.list.fragment.item

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.hyh.page.state.IPageUnitState
import com.hyh.page.state.PageStateController
import com.hyh.page.state.UnitState

abstract class AbsPageUnitItem<VH : RecyclerView.ViewHolder>(
    private val controller: PageStateController
) :
    BaseCardItem<VH>(), IPageUnitState {

    private val stateLiveData by lazy {
        MutableLiveData<Pair<IPageUnitState, UnitState>>(
            Pair(this, initialState)
        )
    }

    abstract val initialState: UnitState

    override fun onItemActivated() {
        super.onItemActivated()
        controller.addPageUnitState(this)
    }

    override fun onItemInactivated() {
        super.onItemInactivated()
        controller.removePageUnitState(this)
    }

    final override fun getState(): UnitState {
        return stateLiveData.value?.second ?: initialState
    }

    final override fun observe(observer: Observer<Pair<IPageUnitState, UnitState>>) {
        stateLiveData.observeForever(observer)
    }

    final override fun removeObserve(observer: Observer<Pair<IPageUnitState, UnitState>>) {
        stateLiveData.removeObserver(observer)
    }

    protected fun notifyStateChanged(newState: UnitState) {
        val oldState = getState()
        if (oldState == newState) return
        stateLiveData.postValue(Pair(this, newState))
    }
}