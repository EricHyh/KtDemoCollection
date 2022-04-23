package com.hyh.paging3demo.widget.horizontal

enum class ScrollUnitState(val state: Int) {

    IDLE(0b0001),
    SCROLL(0b0001 shl 1),
    SETTLING(0b0001 shl 2)

}