package com.hyh.list.decoration

import android.graphics.*
import androidx.annotation.ColorInt
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo

//class SingleSourceCardDecoration(
//    private val outRect: Rect,
//    private val corners: Array<RoundCorner>,
//    colorInt: Int,
//    var supportedSources: List<Any>? = null
//) : BaseRoundCardDecoration(colorInt) {
//
//    constructor(
//        padding: Int,
//        radius: Float,
//        @ColorInt colorInt: Int
//    ) : this(Rect().apply { set(padding, padding, padding, padding) }, radius, colorInt)
//
//    constructor(outRect: Rect, radius: Float, @ColorInt colorInt: Int)
//            : this(outRect, Array<RoundCorner>(4) { RoundCorner(radius) }, colorInt)
//
//    override fun getItemOffsets(
//        outRect: Rect,
//        adapter: IListAdapter<*>,
//        itemLocalInfo: ItemLocalInfo
//    ) {
//        if (!isCardContent(itemLocalInfo.sourceToken)) return
//        if (isCardTopItem(itemLocalInfo)
//            && isCardBottomItem(itemLocalInfo)
//        ) {
//            outRect.set(this.outRect)
//        } else if (isCardTopItem(itemLocalInfo)) {
//            outRect.set(this.outRect)
//            outRect.bottom = 0
//        } else if (isCardBottomItem(itemLocalInfo)) {
//            outRect.set(this.outRect)
//            outRect.top = 0
//        } else {
//            outRect.left = this.outRect.left
//            outRect.right = this.outRect.right
//        }
//    }
//
//    override fun getCardPosition(
//        adapter: IListAdapter<*>,
//        itemLocalInfo: ItemLocalInfo
//    ): Boolean {
//        if (!isCardContent(itemLocalInfo.sourceToken)) return false
//        return if (isCardTopItem(itemLocalInfo)
//            && isCardBottomItem(itemLocalInfo)
//        ) {
//            CardPosition.Whole(corners)
//        } else if (isCardTopItem(itemLocalInfo)) {
//            CardPosition.Top(corners[0], corners[1])
//        } else if (isCardBottomItem(itemLocalInfo)) {
//            CardPosition.Bottom(corners[2], corners[3])
//        } else {
//            CardPosition.Content
//        }
//    }
//
//    private fun isCardContent(sourceToken: Any): Boolean {
//        val supportedSources = this.supportedSources ?: return true
//        return supportedSources.contains(sourceToken)
//    }
//
//    private fun isCardTopItem(itemLocalInfo: ItemLocalInfo): Boolean {
//        return itemLocalInfo.localPosition == 0
//    }
//
//    private fun isCardBottomItem(itemLocalInfo: ItemLocalInfo): Boolean {
//        return itemLocalInfo.localPosition == itemLocalInfo.sourceItemCount - 1
//    }
//}