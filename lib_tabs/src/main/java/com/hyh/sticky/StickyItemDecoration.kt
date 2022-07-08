package com.hyh.sticky

import android.graphics.Canvas
import android.graphics.Rect

interface StickyItemDecoration {

    fun onDrawOver(
        c: Canvas,
        parent: StickyItemsLayout
    )

    fun onDraw(
        c: Canvas,
        parent: StickyItemsLayout
    )

    fun getItemOffsets(
        outRect: Rect,
        adapterPosition: Int,
        parent: StickyItemsLayout
    )
}