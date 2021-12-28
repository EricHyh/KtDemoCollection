package com.hyh.paging3demo.widget.horizontal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout


class ScrollableBehavior(context: Context?, attrs: AttributeSet?) :
    CoordinatorLayout.Behavior<View>(context, attrs) {

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

        return super.onMeasureChild(
            parent,
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed
        )
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
        val dependencies = parent.getDependencies(child)
        var dependenciesWidth = 0
        if (dependencies.isNotEmpty()) {
            for (dependency in dependencies) {
                dependenciesWidth += dependency.measuredWidth
            }
        }
        val width = child.measuredWidth
        val height = child.measuredHeight

        child.layout(120, 0, 120 + width, height)

        return true
    }

}