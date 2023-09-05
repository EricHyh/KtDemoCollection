package com.hyh.paging3demo.widget.horizontal.internal

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.widget.horizontal.ScrollState

class RecyclerViewScrollable(private val recyclerView: RecyclerView) :
    Scrollable<RecyclerViewScrollable.ScrollingData, RecyclerViewScrollable.ScrolledData> {

    private val linearLayoutManager: LinearLayoutManager =
        recyclerView.layoutManager as LinearLayoutManager

    private val diffSize: ScrollingData.DiffSize = ScrollingData.DiffSize(recyclerView)

    private val targetPosition: ScrollingData.TargetPosition = ScrollingData.TargetPosition(recyclerView)

    private val scrolledData: ScrolledData = ScrolledData(recyclerView)

    private var inScrolling = false

    override fun getScrollingData(): ScrollingData {
        val position = linearLayoutManager.findFirstVisibleItemPosition()
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        return targetPosition.apply {
            this.position = position
            this.positionOffset = holder?.itemView?.left
            this.globalOffset = recyclerView.computeHorizontalScrollOffset()
        }
    }

    fun getScrollingData(
        scrollDx: Int,
    ): ScrollingData {
        return diffSize.also {
            it.scrollDx = scrollDx
            it.version = it.version + 1
        }
    }

    override fun getScrolledData(): ScrolledData {
        val position = linearLayoutManager.findFirstVisibleItemPosition()
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        return scrolledData.apply {
            this.position = position
            this.positionOffset = holder?.itemView?.left
            this.globalOffset = recyclerView.computeHorizontalScrollOffset()
        }
    }

    override fun scroll(scrollState: ScrollState, scrollingData: ScrollingData) {
        if (inScrolling) return
        when (scrollingData) {
            is ScrollingData.DiffSize -> {
                if (scrollingData.targetRecyclerView === recyclerView) {
                    return
                }
                inScrolling = true
                recyclerView.scrollBy(scrollingData.scrollDx, 0)
                inScrolling = false
            }
            is ScrollingData.TargetPosition -> {
                if (scrollState == ScrollState.SCROLL || scrollState == ScrollState.SETTLING) {
                    return
                }
                val position = scrollingData.position
                val positionOffset = scrollingData.positionOffset
                if (position >= 0 && positionOffset != null) {
                    inScrolling = true
                    linearLayoutManager.scrollToPositionWithOffset(position, positionOffset)
                    inScrolling = false
                } else {
                    inScrolling = true
                    recyclerView.scrollBy(
                        scrollingData.globalOffset - recyclerView.computeHorizontalScrollOffset(),
                        0
                    )
                    inScrolling = false
                }
            }
        }
    }

    override fun scrollTo(scrolledData: ScrolledData) {
        if (inScrolling) return
        val position = scrolledData.position
        val positionOffset = scrolledData.positionOffset
        if (position >= 0 && positionOffset != null) {
            inScrolling = true
            linearLayoutManager.scrollToPositionWithOffset(position, positionOffset)
            inScrolling = false
        } else {
            inScrolling = true
            recyclerView.scrollBy(
                scrolledData.globalOffset - recyclerView.computeHorizontalScrollOffset(),
                0
            )
            inScrolling = false
        }
    }

    override fun resetScroll() {
        linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    override fun stopScroll() {
        recyclerView.stopScroll()
    }

    sealed interface ScrollingData : IScrollingData {

        data class DiffSize constructor(
            var targetRecyclerView: RecyclerView,
            var scrollDx: Int = 0,
            var version: Int = 0
        ) : ScrollingData {

            override fun clone(): DiffSize {
                return DiffSize(targetRecyclerView, scrollDx, version)
            }

            override fun copy(other: Any): Boolean {
                if (other !is DiffSize) return false
                if (this.targetRecyclerView !== other.targetRecyclerView) return false
                this.scrollDx = other.scrollDx
                this.version = other.version
                return true
            }
        }

        data class TargetPosition constructor(
            val targetRecyclerView: RecyclerView,
            var position: Int = -1,
            var positionOffset: Int? = null,
            var globalOffset: Int = -1
        ) : ScrollingData {
            override fun clone(): TargetPosition {
                return TargetPosition(
                    targetRecyclerView,
                    position,
                    positionOffset,
                    globalOffset
                )
            }

            override fun copy(other: Any): Boolean {
                if (other !is TargetPosition) return false
                if (this.targetRecyclerView !== other.targetRecyclerView) return false
                this.position = other.position
                this.positionOffset = other.positionOffset
                this.globalOffset = other.globalOffset
                return true
            }
        }
    }

    data class ScrolledData constructor(
        val targetRecyclerView: RecyclerView,
        var position: Int = -1,
        var positionOffset: Int? = null,
        var globalOffset: Int = -1
    ) : IScrolledData {
        override fun clone(): ScrolledData {
            return ScrolledData(
                targetRecyclerView,
                position,
                positionOffset,
                globalOffset
            )
        }

        override fun copy(other: Any): Boolean {
            if (other !is ScrolledData) return false
            if (this.targetRecyclerView !== other.targetRecyclerView) return false
            this.position = other.position
            this.positionOffset = other.positionOffset
            this.globalOffset = other.globalOffset
            return true
        }
    }
}