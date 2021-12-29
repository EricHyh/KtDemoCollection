package com.hyh.paging3demo.widget.horizontal

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout


class ScrollableBehavior(var fixedMinWidth: Int) :
    CoordinatorLayout.Behavior<View>() {

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

        val maxWidth = parent.width - fixedMinWidth

        val mode = View.MeasureSpec.getMode(parentWidthMeasureSpec)
        val size = View.MeasureSpec.getSize(parentWidthMeasureSpec)

        val width: Int
        val widthMeasureSpec: Int

        when (mode) {
            View.MeasureSpec.EXACTLY -> {
                width = size.coerceAtMost(maxWidth)
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            }
            View.MeasureSpec.AT_MOST -> {
                width = size.coerceAtMost(maxWidth)
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
            }
            else -> {
                widthMeasureSpec = parentWidthMeasureSpec
            }
        }

        child.measure(widthMeasureSpec, parentHeightMeasureSpec)

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