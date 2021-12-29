package com.hyh.paging3demo.widget.horizontal

import androidx.recyclerview.widget.RecyclerView

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/28
 */
interface Scrollable<T> {

    fun getScrollData(): T

    fun scrollTo(t: T)

    fun resetScroll()

}


class RecyclerViewScrollable(private val recyclerView: RecyclerView) : Scrollable<Int> {

    override fun getScrollData(): Int {
        return recyclerView.computeHorizontalScrollOffset()
    }

    override fun scrollTo(t: Int) {
        recyclerView.scrollBy(t - recyclerView.computeHorizontalScrollOffset(), 0)
    }

    override fun resetScroll() {
        recyclerView.scrollBy(0 - recyclerView.computeHorizontalScrollOffset(), 0)
    }
}