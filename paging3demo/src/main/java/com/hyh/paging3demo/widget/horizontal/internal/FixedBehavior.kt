package com.hyh.paging3demo.widget.horizontal.internal

import android.animation.ValueAnimator
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import kotlin.math.max

internal class FixedBehavior constructor(
    var fixedMinWidth: Int,
    var fixedMaxWidth: Int = Int.MAX_VALUE,
    private val scrollableView: View,
    private val scrollable: Scrollable<out IScrollingData, out IScrolledData>,
    private val onScrollableScrollListener: OnScrollableScrollListener,
    private val isAllowReleaseDrag: () -> Boolean,
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

        child.measure(
            getFixedWidthMeasureSpec(),
            parentHeightMeasureSpec
        )

        scrollableView.measure(
            getScrollableWidthMeasureSpec(parentWidthMeasureSpec),
            parentHeightMeasureSpec
        )

        val fixedViewHeight = child.measuredHeight
        val scrollableViewHeight = scrollableView.measuredHeight
        if (fixedViewHeight != scrollableViewHeight) {
            val height = max(fixedViewHeight, scrollableViewHeight)
            val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                height,
                View.MeasureSpec.EXACTLY
            )
            if (fixedViewHeight < height) {
                child.measure(
                    getFixedWidthMeasureSpec(),
                    heightMeasureSpec
                )
            }
            if (scrollableViewHeight < height) {
                scrollableView.measure(
                    getScrollableWidthMeasureSpec(parentWidthMeasureSpec),
                    heightMeasureSpec
                )
            }
        }

        return true
    }

    private fun getFixedWidthMeasureSpec(): Int {
        val fixedWidth =
            (fixedMinWidth + (currentDragWith * DRAG_RATIO).toInt()).coerceAtMost(fixedMaxWidth)
        return View.MeasureSpec.makeMeasureSpec(fixedWidth, View.MeasureSpec.EXACTLY)
    }

    private fun getScrollableWidthMeasureSpec(parentWidthMeasureSpec: Int): Int {
        val widthMode = View.MeasureSpec.getMode(parentWidthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(parentWidthMeasureSpec)
        val maxWidth = (widthSize - fixedMinWidth).coerceAtLeast(0)

        val width: Int
        val widthMeasureSpec: Int

        when (widthMode) {
            View.MeasureSpec.EXACTLY -> {
                width = widthSize.coerceAtMost(maxWidth)
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            }
            View.MeasureSpec.AT_MOST -> {
                width = widthSize.coerceAtMost(maxWidth)
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
            }
            else -> {
                widthMeasureSpec = parentWidthMeasureSpec
            }
        }
        return widthMeasureSpec
    }


    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
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
                    onScrollableScrollListener.onDragging(currentDragWith)
                } else {
                    onScrollableScrollListener.onScrolling(
                        scrollable.getScrollingData(),
                        scrollable.getScrolledData()
                    )
                }
            }
            ViewCompat.TYPE_NON_TOUCH -> {
                onScrollableScrollListener.onSettling(
                    scrollable.getScrollingData(),
                    scrollable.getScrolledData()
                )
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
            onScrollableScrollListener.onDragging(currentDragWith)
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
            onScrollableScrollListener.onDragging(currentDragWith)
        } else {
            if (type == ViewCompat.TYPE_TOUCH) {
                onScrollableScrollListener.onScrolling(
                    scrollable.getScrollingData(),
                    scrollable.getScrolledData()
                )
            } else {
                onScrollableScrollListener.onSettling(
                    scrollable.getScrollingData(),
                    scrollable.getScrolledData()
                )
            }
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_TOUCH) {
            if (currentDragWith != 0 && isAllowReleaseDrag()) {
                releaseDragHelper.start()
                onScrollableScrollListener.onRebounding(currentDragWith)
            }
        }
        if (currentDragWith == 0) {
            onScrollableScrollListener.onIdle(
                scrollable.getScrolledData()
            )
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
                onScrollableScrollListener.onRebounding(currentDragWith)
            }
        }
    }
}

interface OnScrollableScrollListener : OnScrollEventListener