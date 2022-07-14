package com.hyh.list.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 填充底部的装饰器，让最后一个Item可以滑动到顶部
 *
 * @author eriche 2022/7/13
 */
class FillBottomDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter
        if (adapter == null) {
            super.getItemOffsets(outRect, view, parent, state)
            return
        }
        val itemCount = adapter.itemCount
        val childAdapterPosition = parent.getChildAdapterPosition(view)
        val bottom = (parent.height -  view.height).coerceAtLeast(0)
        if (childAdapterPosition == itemCount - 1) {
            outRect.set(
                0,
                0,
                0,
                bottom
            )
        }
    }
}