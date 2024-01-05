package com.hyh.paging3demo.widget.horizontal.internal

/**
 * 滑动事件监听
 *
 * @author eriche 2023/9/5
 */
interface OnScrollEventListener {

    fun onDragging(currentDragWith: Int)

    fun onRebounding(currentDragWith: Int)

    fun onScrolling(scrollingData: IScrollingData, scrolledData: IScrolledData)

    fun onSettling(scrollingData: IScrollingData, scrolledData: IScrolledData)

    fun onIdle(scrolledData: IScrolledData)

    fun onInitial()
}