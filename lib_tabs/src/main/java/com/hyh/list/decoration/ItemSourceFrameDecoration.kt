package com.hyh.list.decoration

import android.graphics.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo
import kotlin.properties.Delegates

class ItemSourceFrameDecoration(
    outRect: Rect,
    radius: FloatArray,
    colorInt: Int,
    private val supportedSources: List<Any>? = null
) : BaseItemSourceFrameDecoration(outRect, radius, colorInt) {


    constructor(padding: Int, radius: Float, @ColorInt colorInt: Int)
            : this(Rect().apply { set(padding, padding, padding, padding) }, radius, colorInt)

    constructor(outRect: Rect, radius: Float, @ColorInt colorInt: Int)
            : this(outRect, FloatArray(4) { radius }, colorInt)


    override fun shouldDrawOver(sourceToken: Any): Boolean {
        if (supportedSources == null) return true
        return supportedSources.contains(sourceToken)
    }
}