package com.hyh.widget.sticky

import android.graphics.Canvas
import android.graphics.Rect

interface StickyItemDecoration {

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