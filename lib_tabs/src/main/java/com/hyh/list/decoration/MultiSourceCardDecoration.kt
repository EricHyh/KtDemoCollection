package com.hyh.list.decoration

import android.graphics.*
import androidx.annotation.ColorInt
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo

class MultiSourceCardDecoration(
    outRect: Rect,
    radius: FloatArray,
    colorInt: Int,
    supportedSourceGroups: List<List<Any>>? = null
) : BaseItemSourceFrameDecoration(outRect, radius, colorInt) {

    private val supportedSourceGroupsMap: MutableMap<Any, List<Any>> = mutableMapOf()

    init {
        resetSupportedSourceGroupsMap(supportedSourceGroups)
    }

    constructor(padding: Int, radius: Float, @ColorInt colorInt: Int)
            : this(Rect().apply { set(padding, padding, padding, padding) }, radius, colorInt)

    constructor(outRect: Rect, radius: Float, @ColorInt colorInt: Int)
            : this(outRect, FloatArray(4) { radius }, colorInt)

    override fun shouldDrawOver(adapter: IListAdapter<*>, sourceToken: Any): Boolean {
        val sourceGroup = supportedSourceGroupsMap[sourceToken] ?: return false
        return sourceGroup.contains(sourceToken)
    }

    fun setSupportedSourceGroups(supportedSourceGroups: List<List<Any>>?) {
        resetSupportedSourceGroupsMap(supportedSourceGroups)
    }

    override fun isCardTopItem(adapter: IListAdapter<*>, itemLocalInfo: ItemLocalInfo): Boolean {
        if (itemLocalInfo.localPosition != 0) return false
        val sourceGroup = supportedSourceGroupsMap[itemLocalInfo.sourceToken] ?: return false
        if (sourceGroup.isEmpty()) return false
        return sourceGroup.firstOrNull() == itemLocalInfo.sourceToken
    }

    override fun isCardBottomItem(adapter: IListAdapter<*>, itemLocalInfo: ItemLocalInfo): Boolean {
        if (itemLocalInfo.localPosition != itemLocalInfo.sourceItemCount - 1) return false
        val sourceGroup = supportedSourceGroupsMap[itemLocalInfo.sourceToken] ?: return false
        if (sourceGroup.isEmpty()) return false
        return sourceGroup.lastOrNull() == itemLocalInfo.sourceToken
    }

    private fun resetSupportedSourceGroupsMap(supportedSourceGroups: List<List<Any>>?) {
        supportedSourceGroupsMap.clear()
        supportedSourceGroups?.forEach { sourceGroup ->
            sourceGroup.forEach {
                supportedSourceGroupsMap[it] = sourceGroup
            }
        }
    }
}