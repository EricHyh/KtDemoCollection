package com.hyh.page.state

enum class UnitState(val state: Int) {

    EMPTY(0b0001),
    ERROR(0b0001 shl 1),
    LOADING(0b0001 shl 2),
    SUCCESS(0b0001 shl 3),

}

enum class PageState {

    EMPTY,
    ERROR,
    LOADING,
    SUCCESS,

}