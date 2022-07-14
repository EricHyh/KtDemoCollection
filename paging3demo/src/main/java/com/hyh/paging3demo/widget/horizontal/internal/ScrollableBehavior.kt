package com.hyh.paging3demo.widget.horizontal.internal

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout


internal class ScrollableBehavior(
    var fixedMinWidth: Int
) : CoordinatorLayout.Behavior<View>() {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        val layoutParams = dependency.layoutParams as? CoordinatorLayout.LayoutParams
        val behavior = layoutParams?.behavior
        return behavior is FixedBehavior
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        return true
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
        val width = child.measuredWidth
        val height = child.measuredHeight
        child.layout(fixedMinWidth, 0, fixedMinWidth + width, height)
        return true
    }
}