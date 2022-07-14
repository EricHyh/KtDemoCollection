package com.hyh.paging3demo.widget.horizontal.internal

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

    data class RecyclerViewScrollData(
        var position: Int = -1,
        var positionOffset: Int? = null,
        var globalOffset: Int = -1
    ) : IScrollData {
        override fun toString(): String {
            return "RecyclerViewScrollData(position=$position, positionOffset=$positionOffset, globalOffset=$globalOffset)"
        }

        override fun clone(): Any {
            return RecyclerViewScrollData(position, positionOffset, globalOffset)
        }

        override fun copy(other: IScrollData) {
            if (other !is RecyclerViewScrollData) return
            this.position = other.position
            this.positionOffset = other.positionOffset
            this.globalOffset = other.globalOffset
        }
    }

    override fun stopScroll() {
        recyclerView.stopScroll()
    }
}