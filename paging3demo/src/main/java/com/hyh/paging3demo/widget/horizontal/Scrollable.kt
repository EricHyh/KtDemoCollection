package com.hyh.paging3demo.widget.horizontal

import androidx.recyclerview.widget.RecyclerView

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/28
 */
interface Scrollable<T> {

    fun setOnScrollChangeListener(listener: OnScrollChangedListener)

    fun getScrollData(): T

    fun scrollTo(t: T)


    interface OnScrollChangedListener {
        fun onScrollChanged()
    }
}


class RecyclerViewScrollable(private val recyclerView: RecyclerView) : Scrollable<Int> {

    override fun setOnScrollChangeListener(listener: Scrollable.OnScrollChangedListener) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                listener.onScrollChanged()
            }
        })
    }

    override fun getScrollData(): Int {
        return recyclerView.computeHorizontalScrollOffset()
    }

    override fun scrollTo(t: Int) {
        recyclerView.scrollBy(t - recyclerView.computeHorizontalScrollOffset(), 0)
    }
}