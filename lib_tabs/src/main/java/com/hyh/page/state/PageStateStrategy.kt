package com.hyh.page.state

interface IPageStateStrategy {

    fun calculatePageState(unitSates: Collection<UnitState>): PageState

}

class OneSuccessStrategy : IPageStateStrategy {
    override fun calculatePageState(unitSates: Collection<UnitState>): PageState {
        var state = 0b0000
        unitSates.forEach {
            state = state or it.state
        }
        if (state and UnitState.SUCCESS.state != 0) {
            return PageState.SUCCESS
        }
        if (state and UnitState.LOADING.state != 0) {
            return PageState.LOADING
        }
        if (state and UnitState.ERROR.state != 0) {
            return PageState.ERROR
        }
        return PageState.EMPTY
    }
}

class AllSuccessStrategy : IPageStateStrategy {
    override fun calculatePageState(unitSates: Collection<UnitState>): PageState {
        var state = 0b0000
        unitSates.forEach {
            state = state or it.state
        }
        if (state and UnitState.LOADING.state != 0) {
            return PageState.LOADING
        }
        if (state and UnitState.ERROR.state != 0) {
            return PageState.ERROR
        }
        if (state and 0b1111 == UnitState.SUCCESS.state) {
            return PageState.SUCCESS
        }
        return PageState.EMPTY
    }
}