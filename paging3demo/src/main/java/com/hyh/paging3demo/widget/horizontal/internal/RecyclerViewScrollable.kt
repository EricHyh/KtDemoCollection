package com.hyh.paging3demo.widget.horizontal.internal

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.widget.horizontal.ScrollState

class RecyclerViewScrollable(private val recyclerView: RecyclerView) :
    Scrollable<RecyclerViewScrollable.RecyclerViewScrollData> {

    private val linearLayoutManager: LinearLayoutManager =
        recyclerView.layoutManager as LinearLayoutManager

    private val calculateScrollData: RecyclerViewScrollData.CalculateScrollData =
        RecyclerViewScrollData.CalculateScrollData(recyclerView)

    private val scrollData: RecyclerViewScrollData.ScrolledData =
        RecyclerViewScrollData.ScrolledData(recyclerView)

    private var inScrolling = false

    override fun getScrollData(): RecyclerViewScrollData {
        val position = linearLayoutManager.findFirstVisibleItemPosition()
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        return calculateScrollData.apply {
            this.position = position
            this.positionOffset = holder?.itemView?.left
            this.globalOffset = recyclerView.computeHorizontalScrollOffset()
        }
    }

    fun getScrollData(
        scrollDx: Int,
    ): RecyclerViewScrollData.ScrolledData {
        return scrollData.also {
            it.scrollDx = scrollDx
            it.version = it.version + 1
        }
    }

    override fun scrollTo(scrollState: ScrollState, t: RecyclerViewScrollData) {
        if (inScrolling) return
        when (t) {
            is RecyclerViewScrollData.ScrolledData -> {
                if (t.targetRecyclerView === recyclerView) {
                    return
                }
                inScrolling = true
                recyclerView.scrollBy(t.scrollDx, 0)
                inScrolling = false
            }
            is RecyclerViewScrollData.CalculateScrollData -> {
                if (scrollState == ScrollState.SCROLL || scrollState == ScrollState.SETTLING) {
                    return
                }
                val position = t.position
                val positionOffset = t.positionOffset
                if (position >= 0 && positionOffset != null) {
                    inScrolling = true
                    linearLayoutManager.scrollToPositionWithOffset(position, positionOffset)
                    inScrolling = false
                } else {
                    inScrolling = true
                    recyclerView.scrollBy(
                        t.globalOffset - recyclerView.computeHorizontalScrollOffset(),
                        0
                    )
                    inScrolling = false
                }
            }
        }
    }

    override fun resetScroll() {
        linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    sealed interface RecyclerViewScrollData : IScrollData {

        data class ScrolledData constructor(
            var targetRecyclerView: RecyclerView,
            var scrollDx: Int = 0,
            var version: Int = 0
        ) : RecyclerViewScrollData {

            override fun clone(): Any {
                return ScrolledData(targetRecyclerView, scrollDx, version)
            }

            override fun copy(other: IScrollData): Boolean {
                if (other !is ScrolledData) return false
                if (this.targetRecyclerView !== other.targetRecyclerView) return false
                this.scrollDx = other.scrollDx
                this.version = other.version
                return true
            }
        }

        data class CalculateScrollData constructor(
            val targetRecyclerView: RecyclerView,
            var position: Int = -1,
            var positionOffset: Int? = null,
            var globalOffset: Int = -1
        ) : RecyclerViewScrollData {
            override fun clone(): Any {
                return CalculateScrollData(
                    targetRecyclerView,
                    position,
                    positionOffset,
                    globalOffset
                )
            }

            override fun copy(other: IScrollData): Boolean {
                if (other !is CalculateScrollData) return false
                if (this.targetRecyclerView !== other.targetRecyclerView) return false
                this.position = other.position
                this.positionOffset = other.positionOffset
                this.globalOffset = other.globalOffset
                return true
            }
        }
    }

    override fun stopScroll() {
        recyclerView.stopScroll()
    }
}