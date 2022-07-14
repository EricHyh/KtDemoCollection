package com.hyh.paging3demo.widget.horizontal.internal

internal class NestedHorizontalScrollable(private val nestedScrollView: NestedHorizontalScrollView) :
    Scrollable<NestedHorizontalScrollable.ScrollData> {

    override fun getScrollData(): ScrollData {
        return ScrollData(
            nestedScrollView.scrollX
        )
    }

    override fun scrollTo(t: ScrollData) {
        nestedScrollView.scrollTo(t.scrollX, 0)
    }

    override fun resetScroll() {
        nestedScrollView.scrollTo(0, 0)
    }

    override fun stopScroll() {
        nestedScrollView.stopScroll()
    }

    data class ScrollData(
        var scrollX: Int = 0
    ) : IScrollData {

        override fun toString(): String {
            return "ScrollData(scrollX=$scrollX)"
        }

        override fun clone(): Any {
            return ScrollData(scrollX)
        }

        override fun copy(other: IScrollData) {
            if (other !is ScrollData) return
            this.scrollX = other.scrollX
        }
    }
}