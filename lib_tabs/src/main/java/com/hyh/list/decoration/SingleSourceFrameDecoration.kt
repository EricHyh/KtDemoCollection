package com.hyh.list.decoration

import android.graphics.*
import androidx.annotation.ColorInt
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo

class SingleSourceFrameDecoration(
    outRect: Rect,
    radius: FloatArray,
    colorInt: Int,
    var supportedSources: List<Any>? = null
) : BaseItemSourceFrameDecoration(outRect, radius, colorInt) {

    constructor(padding: Int, radius: Float, @ColorInt colorInt: Int)
            : this(Rect().apply { set(padding, padding, padding, padding) }, radius, colorInt)

    constructor(outRect: Rect, radius: Float, @ColorInt colorInt: Int)
            : this(outRect, FloatArray(4) { radius }, colorInt)

    override fun shouldDrawOver(adapter: IListAdapter<*>, sourceToken: Any): Boolean {
        val supportedSources = this.supportedSources ?: return true
        return supportedSources.contains(sourceToken)
    }

    override fun isFirstItem(adapter: IListAdapter<*>, itemLocalInfo: ItemLocalInfo): Boolean {
        return itemLocalInfo.localPosition == 0
    }

    override fun isLastItem(adapter: IListAdapter<*>, itemLocalInfo: ItemLocalInfo): Boolean {
        return itemLocalInfo.localPosition == itemLocalInfo.sourceItemCount - 1
    }
}