package com.hyh.paging3demo.widget.horizontal

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat

internal class FixedBehavior constructor(
    var fixedMinWidth: Int,
    var fixedMaxWidth: Int = Int.MAX_VALUE,
    private val scrollable: Scrollable<out IScrollData>,
    private val onScrollChanged: (scrollState: ScrollState, data: Any) -> Unit,
    private val onStopScroll: () -> Unit,
) : CoordinatorLayout.Behavior<View>() {

    companion object {
        private const val TAG = "FixedBehavior"

        private const val DRAG_RATIO = 0.6F
    }


    private var parent: View? = null
    private var child: View? = null

    var currentDragWith: Int = 0
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                field = value
                child?.requestLayout()
                parent?.scrollTo(-(value * DRAG_RATIO).toInt(), 0)
            }
        }

    private val releaseDragHelper: ReleaseDragHelper by lazy {
        ReleaseDragHelper()
    }

    fun stopScroll() {
        releaseDragHelper.stop()
        scrollable.stopScroll()
    }

    fun tryRebound() {
        if (currentDragWith != 0) {
            releaseDragHelper.start()
        }
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        this.parent = parent
        this.child = child
        val fixedWidth =
            (fixedMinWidth + (currentDragWith * DRAG_RATIO).toInt()).coerceAtMost(fixedMaxWidth)
        val fixedWidthMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(fixedWidth, View.MeasureSpec.EXACTLY)
        child.measure(fixedWidthMeasureSpec, parentHeightMeasureSpec)
        return true
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: View, layoutDirection: Int): Boolean {
        val height = child.measuredHeight
        child.layout(fixedMinWidth - child.measuredWidth, 0, fixedMinWidth, height)
        return true
    }


    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return true
    }

    override fun onNestedScrollAccepted(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ) {
        when (type) {
            ViewCompat.TYPE_TOUCH -> {
                releaseDragHelper.stop()
                if (currentDragWith != 0) {
                    onScrollChanged(ScrollState.DRAG, currentDragWith)
                } else {
                    onScrollChanged(ScrollState.SCROLL, scrollable.getScrollData())
                }
            }
            ViewCompat.TYPE_NON_TOUCH -> {
                onScrollChanged(ScrollState.SETTLING, scrollable.getScrollData())
            }
        }
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (dx < 0) {
            if (target.canScrollHorizontally(dx)) return
            if (type == ViewCompat.TYPE_TOUCH) {
                currentDragWith -= dx
                consumed[0] = dx
            }
        } else {
            if (currentDragWith > 0) {
                if (currentDragWith - dx > 0) {
                    currentDragWith -= dx
                    consumed[0] = dx
                } else {
                    val consumedX = currentDragWith
                    currentDragWith = 0
                    consumed[0] = consumedX
                }
            }
        }
        if (currentDragWith != 0) {
            onScrollChanged(ScrollState.DRAG, currentDragWith)
        }
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (dxUnconsumed < 0) {
            if (type == ViewCompat.TYPE_TOUCH) {
                currentDragWith -= dxUnconsumed
            }
        } else {
            if (currentDragWith > 0) {
                if (currentDragWith - dxUnconsumed > 0) {
                    currentDragWith -= dxUnconsumed
                } else {
                    currentDragWith = 0
                }
            }
        }
        if (currentDragWith != 0) {
            onScrollChanged(ScrollState.DRAG, currentDragWith)
        } else {
            onScrollChanged(ScrollState.SCROLL, scrollable.getScrollData())
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            if (currentDragWith != 0) {
                releaseDragHelper.start()
                onScrollChanged(ScrollState.REBOUND, currentDragWith)
            }
        }
        if (currentDragWith == 0) {
            onScrollChanged(ScrollState.IDLE, scrollable.getScrollData())
        }
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return currentDragWith != 0
    }

    inner class ReleaseDragHelper : ValueAnimator.AnimatorUpdateListener {

        private var animator: ValueAnimator? = null

        fun start() {
            val oldAnimator = animator
            if (oldAnimator != null && oldAnimator.isRunning) {
                oldAnimator.removeUpdateListener(this)
                oldAnimator.cancel()
            }
            animator = ValueAnimator.ofInt(currentDragWith, 0).setDuration(300).apply {
                addUpdateListener(this@ReleaseDragHelper)
            }
            animator?.start()
        }

        fun stop() {
            animator?.removeUpdateListener(this)
            if (animator?.isRunning == true) {
                animator?.cancel()
            }
        }

        override fun onAnimationUpdate(animation: ValueAnimator) {
            if (animator === animation) {
                currentDragWith = animation.animatedValue as Int
                onScrollChanged(ScrollState.REBOUND, currentDragWith)
            }
        }
    }
}