package com.hyh.widget.sticky

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View

interface StickyHeaderDecoration {

    fun onDraw(
        c: Canvas,
        parent: StickyHeadersLayout
    )

    fun getHeaderOffsets(
        outRect: Rect,
        headerView: View,
        adapterPosition: Int,
        parent: StickyHeadersLayout
    )
}