package com.hyh.page.state

import androidx.lifecycle.Observer

interface IPageUnitState {

    fun getState(): UnitState

    fun observe(observer: Observer<Pair<IPageUnitState, UnitState>>)

    fun removeObserve(observer: Observer<Pair<IPageUnitState, UnitState>>)

}