package com.hyh.paging3demo.widget.behavior

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat

class FixedBehavior(context: Context?, attrs: AttributeSet?) :
    CoordinatorLayout.Behavior<View>(context, attrs) {

    companion object {
        private const val TAG = "FixedBehavior"

        private const val DRAG_RATIO = 0.6F
    }

    private var fixedMinWidth = 120
    private var fixedMaxWidth = Int.MAX_VALUE

    private var parent: View? = null
    private var child: View? = null


    private var currentDragWith: Int = 0
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                //Log.d(TAG, "currentBounceWith: ${field - value}")
                field = value
                child?.requestLayout()
                parent?.scrollTo(-(value * DRAG_RATIO).toInt(), 0)
            }
        }

    private val releaseDragHelper: ReleaseDragHelper by lazy {
        ReleaseDragHelper()
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return super.layoutDependsOn(parent, child, dependency)
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
            (fixedMinWidth /*+ (currentDragWith * DRAG_RATIO).toInt()*/).coerceAtMost(fixedMaxWidth)
        val fixedWidthMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(fixedWidth, View.MeasureSpec.EXACTLY)
        child.measure(fixedWidthMeasureSpec, parentHeightMeasureSpec)
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
        if (type == ViewCompat.TYPE_TOUCH) {
            releaseDragHelper.stop()
        }
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
        super.onNestedScrollAccepted(
            coordinatorLayout,
            child,
            directTargetChild,
            target,
            axes,
            type
        )
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
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH && currentDragWith > 0) {
            releaseDragHelper.start()
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
            }
        }
    }
}