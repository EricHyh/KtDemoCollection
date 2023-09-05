package com.hyh.paging3demo.widget.horizontal.internal

import com.hyh.paging3demo.widget.horizontal.ScrollState

internal class NestedHorizontalScrollable(private val nestedScrollView: NestedHorizontalScrollView) :
    Scrollable<NestedHorizontalScrollable.ScrollData, NestedHorizontalScrollable.ScrollData> {

    override fun getScrollingData(): ScrollData {
        return ScrollData(
            nestedScrollView.scrollX
        )
    }

    override fun getScrolledData(): ScrollData {
        return ScrollData(
            nestedScrollView.scrollX
        )
    }

    override fun scroll(scrollState: ScrollState, scrollingData: ScrollData) {
        nestedScrollView.scrollTo(scrollingData.scrollX, 0)
    }

    override fun scrollTo(scrolledData: ScrollData) {
        nestedScrollView.scrollTo(scrolledData.scrollX, 0)
    }

    override fun resetScroll() {
        nestedScrollView.scrollTo(0, 0)
    }

    override fun stopScroll() {
        nestedScrollView.stopScroll()
    }

    data class ScrollData constructor(
        var scrollX: Int = 0
    ) : IScrollingData, IScrolledData {

        override fun toString(): String {
            return "ScrollData(scrollX=$scrollX)"
        }

        override fun clone(): ScrollData {
            return ScrollData(scrollX)
        }

        override fun copy(other: Any): Boolean {
            if (other !is ScrollData) return false
            this.scrollX = other.scrollX
            return true
        }
    }
}