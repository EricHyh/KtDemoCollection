package com.hyh.paging3demo.widget.horizontal.internal

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 水平方向支持嵌套滑动的View
 *
 * @author eriche 2022/7/12
 */
class NestedHorizontalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle), NestedScrollingChild3 {
    companion object {
        private const val TAG = "NestedHorizontal"

        /**
         * The RecyclerView is not currently scrolling.
         */
        const val SCROLL_STATE_IDLE = 0

        /**
         * The RecyclerView is currently being dragged by outside input such as user touch input.
         */
        const val SCROLL_STATE_DRAGGING = 1

        /**
         * The RecyclerView is currently animating to a final position while not under
         * outside control.
         */
        const val SCROLL_STATE_SETTLING = 2

        private val quinticInterpolator = Interpolator { input ->
            var t = input
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }

    private var contentWidth: Int = 0
        set(value) {
            if (field == value) return
            val oldValue = field
            field = value
            onContentWidthChanged(oldValue, value)
        }


    // region scroll field

    private val childHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

    private val touchSlop: Int
    private var overTouchSlop: Boolean = false
    private val minFlingVelocity: Int
    private val maxFlingVelocity: Int

    private val mViewFlinger: ViewFlinger = ViewFlinger()
    private var velocityTracker: VelocityTracker? = null

    private var scrollPointerId = 0

    private var initialTouchX = 0
    private var initialTouchY = 0
    private var lastTouchX = 0
    private var lastTouchY = 0

    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private val nestedOffsets = IntArray(2)

    private var _scrollState: Int = SCROLL_STATE_IDLE
    val scrollState: Int
        get() = _scrollState

    // endregion


    init {
        val configuration = ViewConfiguration.get(getContext())
        touchSlop = configuration.scaledTouchSlop
        minFlingVelocity = configuration.scaledMinimumFlingVelocity
        maxFlingVelocity = configuration.scaledMaximumFlingVelocity
        isNestedScrollingEnabled = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        var contentWidth = 0
        var maxHeight = 0
        var childState = 0

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val layoutParams = child.layoutParams as LayoutParams
            measureChildWithMargins(
                child,
                widthMeasureSpec,
                paddingLeft + paddingRight,
                heightMeasureSpec,
                paddingTop + paddingBottom
            )
            contentWidth += layoutParams.leftMargin + child.measuredWidth + layoutParams.rightMargin

            maxHeight = max(maxHeight, child.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin)

            childState = combineMeasuredStates(childState, child.measuredState)
        }

        setMeasuredDimension(
            resolveSizeAndState(contentWidth, widthMeasureSpec, childState),
            resolveSizeAndState(maxHeight, heightMeasureSpec, childState)
        )
        this.contentWidth = contentWidth
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childCount = childCount
        var childLeft = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val layoutParams = child.layoutParams as LayoutParams
            val left = childLeft + layoutParams.leftMargin
            val top = 0
            val right = left + child.measuredWidth
            val bottom = child.measuredHeight
            child.layout(left, top, right, bottom)
            childLeft = right + layoutParams.rightMargin
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker?.addMovement(ev)


        val action = ev.actionMasked
        val actionIndex = ev.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                overTouchSlop = false
                scrollPointerId = ev.getPointerId(0)

                initialTouchX = ev.x.roundToInt()
                lastTouchX = initialTouchX

                initialTouchY = ev.y.roundToInt()
                lastTouchY = initialTouchY

                if (scrollState == SCROLL_STATE_SETTLING) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    setScrollState(SCROLL_STATE_DRAGGING)
                    stopNestedScroll(TYPE_NON_TOUCH)
                }

                nestedOffsets[0] = 0
                nestedOffsets[1] = 0

                var nestedScrollAxis: Int = ViewCompat.SCROLL_AXIS_NONE
                nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_HORIZONTAL
                startNestedScroll(nestedScrollAxis, TYPE_TOUCH)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                scrollPointerId = ev.getPointerId(actionIndex)

                initialTouchX = ev.getX(actionIndex).roundToInt()
                lastTouchX = initialTouchX

                initialTouchY = ev.getY(actionIndex).roundToInt()
                lastTouchY = initialTouchY
            }
            MotionEvent.ACTION_MOVE -> {
                val index: Int = ev.findPointerIndex(scrollPointerId)
                if (index < 0) {
                    Log.e(
                        TAG, "Error processing scroll; pointer index for id "
                                + scrollPointerId + " not found. Did any MotionEvents get skipped?"
                    )
                    return false
                }

                val x = ev.getX(index).roundToInt()
                val y = ev.getY(index).roundToInt()
                if (!overTouchSlop && scrollState != SCROLL_STATE_DRAGGING) {
                    val dx: Int = x - initialTouchX
                    val dy: Int = y - initialTouchY
                    var startScroll = false
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        if (abs(dx) > abs(dy)) {
                            lastTouchX = x
                            lastTouchY = y
                            startScroll = true
                        }
                        overTouchSlop = true
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onPointerUp(ev)
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.clear()
                stopNestedScroll(TYPE_TOUCH)
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelTouch()
            }
        }

        return scrollState == SCROLL_STATE_DRAGGING
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker?.addMovement(ev)

        val action = ev.actionMasked
        val actionIndex = ev.actionIndex

        if (action == MotionEvent.ACTION_DOWN) {
            velocityTracker?.clear()
            mViewFlinger.stop()
        }

        var eventAddedToVelocityTracker = false
        val vtev = MotionEvent.obtain(ev)
        if (action == MotionEvent.ACTION_DOWN) {
            nestedOffsets[0] = 0
            nestedOffsets[1] = 0
        }
        vtev.offsetLocation(nestedOffsets[0].toFloat(), nestedOffsets[1].toFloat())

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                scrollPointerId = ev.getPointerId(actionIndex)
                initialTouchX = ev.getX(actionIndex).roundToInt()
                initialTouchY = ev.getX(actionIndex).roundToInt()
                lastTouchX = initialTouchX
                lastTouchY = initialTouchY
                startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL, TYPE_TOUCH)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                scrollPointerId = ev.getPointerId(actionIndex)
                initialTouchX = ev.getX(actionIndex).roundToInt()
                initialTouchY = ev.getX(actionIndex).roundToInt()
                lastTouchX = initialTouchX
                lastTouchY = initialTouchY
            }

            MotionEvent.ACTION_MOVE -> {
                val index: Int = ev.findPointerIndex(scrollPointerId)
                if (index < 0) {
                    Log.e(
                        TAG, "Error processing scroll; pointer index for id "
                                + scrollPointerId + " not found. Did any MotionEvents get skipped?"
                    )
                    return false
                }

                val x = ev.getX(index).roundToInt()
                val y = ev.getY(index).roundToInt()

                var dx: Int = lastTouchX - x
                var dy: Int = lastTouchY - y

                if (dispatchNestedPreScroll(dx, dy, scrollConsumed, scrollOffset, TYPE_TOUCH)) {
                    dx -= scrollConsumed[0]
                    dy -= scrollConsumed[1]
                    //vtev.offsetLocation(scrollOffset[0].toFloat(), scrollOffset[1].toFloat())
                    // Updated the nested offsets
                    nestedOffsets[0] += scrollOffset[0]
                    nestedOffsets[1] += scrollOffset[1]

                    // Scroll has initiated, prevent parents from intercepting
                    parent.requestDisallowInterceptTouchEvent(true)
                }

                if (scrollState != SCROLL_STATE_DRAGGING) {
                    if (abs(dx) > touchSlop) {
                        if (dx > 0) {
                            dx -= touchSlop
                        } else {
                            dx += touchSlop
                        }
                    }
                    setScrollState(SCROLL_STATE_DRAGGING)
                }

                if (scrollState == SCROLL_STATE_DRAGGING) {
                    lastTouchX = x - scrollOffset[0]
                    lastTouchY = y - scrollOffset[1]
                    if (scrollByInternal(dx, 0, vtev)) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                onPointerUp(ev)
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(vtev)
                eventAddedToVelocityTracker = true
                velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                val xvel: Float = -(velocityTracker?.getXVelocity(scrollPointerId) ?: 0.0F)
                val yvel: Float = 0.0F
                if (!(xvel != 0f && fling(xvel.toInt(), yvel.toInt()))) {
                    setScrollState(SCROLL_STATE_IDLE)
                }
                resetTouch()
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelTouch()
            }
        }
        if (!eventAddedToVelocityTracker) {
            velocityTracker?.addMovement(vtev)
        }
        vtev.recycle()
        return true
    }

    private fun onPointerUp(e: MotionEvent) {
        val actionIndex = e.actionIndex
        if (e.getPointerId(actionIndex) == scrollPointerId) {
            // Pick a new pointer to pick up the slack.
            val newIndex = if (actionIndex == 0) 1 else 0
            scrollPointerId = e.getPointerId(newIndex)
            lastTouchX = (e.getX(newIndex) + 0.5f).toInt()
            initialTouchX = lastTouchX
            lastTouchY = (e.getY(newIndex) + 0.5f).toInt()
            initialTouchY = lastTouchY
        }
    }

    private fun cancelTouch() {
        resetTouch()
        setScrollState(SCROLL_STATE_IDLE)
    }

    private fun resetTouch() {
        if (velocityTracker != null) {
            velocityTracker?.clear()
        }
        stopNestedScroll(TYPE_TOUCH)
    }


    private fun setScrollState(state: Int) {
        if (state == scrollState) {
            return
        }
        _scrollState = state
        if (state != SCROLL_STATE_SETTLING) {
            stopScrollersInternal()
        }
    }


    fun stopScroll() {
        setScrollState(SCROLL_STATE_IDLE)
        stopScrollersInternal()
    }


    private fun stopScrollersInternal() {
        mViewFlinger.stop()
    }

    private fun scrollByInternal(x: Int, y: Int, ev: MotionEvent?): Boolean {
        val unconsumedX: Int
        val unconsumedY: Int = 0
        val consumedX: Int
        val consumedY: Int = 0
        var targetScrollX = x
        targetScrollX = if (x > 0) { //向左滑动
            val curMaxScrollX: Int = contentWidth - measuredWidth - scrollX
            targetScrollX.coerceAtMost(curMaxScrollX)
        } else { //向下滑动
            val curMinScrollX: Int = -scrollX
            targetScrollX.coerceAtLeast(curMinScrollX)
        }
        scrollBy(targetScrollX, 0)
        consumedX = targetScrollX
        unconsumedX = x - targetScrollX
        if (dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, scrollOffset, TYPE_TOUCH)) {
            // Update the last touch co-ords, taking any scroll offset into account
            lastTouchX -= scrollOffset[0]
            lastTouchY -= scrollOffset[1]
            ev?.offsetLocation(scrollOffset[0].toFloat(), scrollOffset[1].toFloat())
            nestedOffsets[0] += scrollOffset[0]
            nestedOffsets[1] += scrollOffset[1]
        }
        if (!awakenScrollBars()) {
            invalidate()
        }
        return consumedX != 0
    }

    private fun fling(inputVelocityX: Int, inputVelocityY: Int): Boolean {
        var velocityX = inputVelocityX
        var velocityY = inputVelocityY
        velocityY = 0
        if (abs(velocityX) < minFlingVelocity) {
            velocityX = 0
        }
        if (velocityX == 0) {
            // If we don't have any velocity, return false
            return false
        }
        if (!dispatchNestedPreFling(velocityX.toFloat(), velocityY.toFloat())) {
            val canScroll = true
            dispatchNestedFling(velocityX.toFloat(), velocityY.toFloat(), canScroll)
            var nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE
            nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_VERTICAL
            startNestedScroll(nestedScrollAxis, TYPE_NON_TOUCH)
            velocityX = (-maxFlingVelocity).coerceAtLeast(velocityX.coerceAtMost(maxFlingVelocity))
            velocityY = (-maxFlingVelocity).coerceAtLeast(velocityY.coerceAtMost(maxFlingVelocity))
            mViewFlinger.fling(velocityX, velocityY)
            return true
        }
        return false
    }

    override fun computeHorizontalScrollRange(): Int {
        return max(contentWidth, width)
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return super.canScrollHorizontally(direction)
    }


    private fun onContentWidthChanged(oldValue: Int, newValue: Int) {
        if (newValue > oldValue) return
        val range = max(newValue, measuredWidth) - measuredWidth
        scrollX = min(scrollX, range)
    }

    // region LayoutParams

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p != null && p is LayoutParams
    }

    class LayoutParams : MarginLayoutParams {

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(layoutParams: MarginLayoutParams) : super(layoutParams)

        constructor(layoutParams: LayoutParams) : super(layoutParams)

        constructor(layoutParams: ViewGroup.LayoutParams) : super(layoutParams)

    }


    // endregion


    // region nested

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }


    override fun isNestedScrollingEnabled(): Boolean {
        return childHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return childHelper.startNestedScroll(axes)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll() {
        childHelper.stopNestedScroll()
    }

    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return childHelper.hasNestedScrollingParent()
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    // endregion


    private inner class ViewFlinger : Runnable {
        private var lastFlingX = 0
        private var lastFlingY = 0
        private val scroller: OverScroller
        var interpolator: Interpolator = quinticInterpolator

        // When set to true, postOnAnimation callbacks are delayed until the run method completes
        private var mEatRunOnAnimationRequest = false

        // Tracks if postAnimationCallback should be re-attached when it is done
        private var mReSchedulePostAnimationCallback = false
        override fun run() {
            disableRunOnAnimationRequests()
            // keep a local reference so that if it is changed during onAnimation method, it won't
            // cause unexpected behaviors
            val scroller = scroller
            if (scroller.computeScrollOffset()) {
                val scrollConsumed: IntArray = scrollConsumed
                val x = scroller.currX
                val y = scroller.currY
                var dx = x - lastFlingX
                var dy = y - lastFlingY
                var hresult = 0
                val vresult = 0
                lastFlingX = x
                lastFlingY = y
                var overscrollX = 0
                val overscrollY = 0
                if (dispatchNestedPreScroll(dx, dy, scrollConsumed, null, TYPE_NON_TOUCH)) {
                    dx -= scrollConsumed[0]
                    dy -= scrollConsumed[1]
                }
                var targetScrollX = dx

                targetScrollX = if (dx > 0) { //向左滑动
                    val curMaxScrollX: Int = contentWidth - measuredWidth - scrollX
                    targetScrollX.coerceAtMost(curMaxScrollX)
                } else { //向下滑动
                    val curMinScrollX: Int = -scrollX
                    targetScrollX.coerceAtLeast(curMinScrollX)
                }

                scrollBy(targetScrollX, 0)
                hresult = targetScrollX
                overscrollX = dx - targetScrollX
                if (!dispatchNestedScroll(
                        hresult, vresult, overscrollX, overscrollY, null,
                        TYPE_NON_TOUCH
                    ) && overscrollX != 0
                ) {
                    val vel = scroller.currVelocity.toInt()
                    var velX = 0
                    val velY = 0
                    if (overscrollX != x) {
                        velX = if (overscrollX < 0) -vel else vel
                    }
                    if ((overscrollX == x || scroller.finalX == 0) && (velY != 0 || overscrollY == y || scroller.finalY == 0)) {
                        scroller.abortAnimation()
                    }
                }
                if (!awakenScrollBars()) {
                    invalidate()
                }
                val fullyConsumedHorizontal = dx != 0 && hresult == dx
                val fullyConsumedAny = dx == 0 && dy == 0 || fullyConsumedHorizontal
                if (scroller.isFinished || (!fullyConsumedAny
                            && !hasNestedScrollingParent(TYPE_NON_TOUCH))
                ) {
                    // setting state to idle will stop this.
                    setScrollState(SCROLL_STATE_IDLE)
                    stopNestedScroll(TYPE_NON_TOUCH)
                } else {
                    postOnAnimation()
                }
            }
            // call this after the onAnimation is complete not to have inconsistent callbacks etc.
            enableRunOnAnimationRequests()
        }

        private fun disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false
            mEatRunOnAnimationRequest = true
        }

        private fun enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation()
            }
        }

        fun postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true
            } else {
                removeCallbacks(this)
                ViewCompat.postOnAnimation(this@NestedHorizontalScrollView, this)
            }
        }

        fun fling(velocityX: Int, velocityY: Int) {
            setScrollState(SCROLL_STATE_SETTLING)
            lastFlingY = 0
            lastFlingX = 0
            scroller.fling(0, 0, velocityX, velocityY, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
            postOnAnimation()
        }

        fun stop() {
            removeCallbacks(this)
            scroller.abortAnimation()
        }

        init {
            scroller = OverScroller(context, interpolator)
        }
    }
}
