package com.hyh.paging3demo.widget.horizontal

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 可滑动控件接口描述
 *
 * @author eriche 2021/12/28
 */
interface Scrollable<T> {

    fun getScrollData(): T

    fun scrollTo(t: T)

    fun resetScroll()

    fun stopScroll()

}

class RecyclerViewScrollable(private val recyclerView: RecyclerView) : Scrollable<RecyclerViewScrollable.RecyclerViewScrollData> {

    companion object {
        private const val TAG = "RecyclerViewScrollable"
    }

    private val linearLayoutManager: LinearLayoutManager = recyclerView.layoutManager as LinearLayoutManager

    private val recyclerViewScrollData: RecyclerViewScrollData = RecyclerViewScrollData()

    override fun getScrollData(): RecyclerViewScrollData {
        val position = linearLayoutManager.findFirstVisibleItemPosition()
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        return recyclerViewScrollData.apply {
            this.position = position
            this.positionOffset = holder?.itemView?.left
            this.globalOffset = if (position >= 0 && holder?.itemView?.left != null) -1 else recyclerView.computeHorizontalScrollOffset()
        }
    }

    override fun scrollTo(t: RecyclerViewScrollData) {
        val position = t.position
        val positionOffset = t.positionOffset
        if (position >= 0 && positionOffset != null) {
            linearLayoutManager.scrollToPositionWithOffset(position, positionOffset)
        } else {
            recyclerView.scrollBy(t.globalOffset - recyclerView.computeHorizontalScrollOffset(), 0)
        }
    }

    override fun resetScroll() {
        linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    class RecyclerViewScrollData(
        var position: Int = -1,
        var positionOffset: Int? = null,
        var globalOffset: Int = -1
    )

    override fun stopScroll() {
        recyclerView.stopScroll()
    }
}