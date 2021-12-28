package com.hyh.paging3demo.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.min


class OldHorizontalScrollLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    companion object {
        private const val TAG = "HorizontalScrollLayout"
        const val SCROLL_STATE_IDLE = 0
        const val SCROLL_STATE_DRAGGING = 1
        const val SCROLL_STATE_SETTLING = 2

        private const val BOUNCE_RATIO = 0.6F

        private val quinticInterpolator =
            Interpolator { t ->
                var temp = t
                temp -= 1.0f
                temp * temp * temp * temp * temp + 1.0f
            }
    }

    private lateinit var fixedView: View
    private lateinit var scrollableView: View

    private var fixedMinWidth = 0
    private var fixedMaxWidth = Int.MAX_VALUE

    private var scrollableMaxWidth = 0

    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    private val minimumVelocity by lazy {
        ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }
    private val maximumVelocity by lazy {
        ViewConfiguration.get(context).scaledMaximumFlingVelocity
    }

    private var scrollState = SCROLL_STATE_IDLE
    private var isBeingDragged = false
    private var isUnableToDrag = false

    private var currentDragWith = 0
        set(value) {
            val oldValue = field
            if (oldValue != value) {
                Log.d(TAG, "currentBounceWith: ${field - value}")
                field = value
                requestLayout()
            }
        }

    private var initialTouchX = 0F
    private var initialTouchY = 0F
    private var lastTouchX = 0F
    private var lastTouchY = 0F

    private var velocityTracker: VelocityTracker? = null
    private val viewFlinger: ViewFlinger by lazy {
        ViewFlinger()
    }
    private val releaseDragHelper: ReleaseDragHelper by lazy {
        ReleaseDragHelper()
    }

    private val scrollListeners: List<OnScrollListener>? = null

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.HorizontalScrollLayout,
                defStyle,
                0
            )
            fixedMinWidth = typedArray.getDimensionPixelOffset(
                R.styleable.HorizontalScrollLayout_fixed_min_width,
                0
            )
            fixedMaxWidth = typedArray.getDimensionPixelOffset(
                R.styleable.HorizontalScrollLayout_fixed_max_width,
                Int.MAX_VALUE
            )
            typedArray.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        fixedView = get(0)
        scrollableView = get(1)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isUnableToDrag = pointView(fixedView, event)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isUnableToDrag = false
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (isUnableToDrag) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.x
                initialTouchY = event.y

                lastTouchX = event.x
                lastTouchY = event.y
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val tx = x - initialTouchX
                val ty = y - initialTouchY

                lastTouchX = x
                lastTouchY = y

                if (abs(tx) > touchSlop && abs(tx) * 0.5 > abs(ty)) {
                    isBeingDragged = true
                    requestDisallowInterceptTouchEvent(true)
                    setScrollState(SCROLL_STATE_DRAGGING)
                } else if (abs(ty) > touchSlop) {
                    isUnableToDrag = true
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {

            }
        }
        return isBeingDragged
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isUnableToDrag) return false

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)


        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.x
                initialTouchY = event.y

                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y

                if (isBeingDragged) {
                    val dx = x - lastTouchX
                    lastTouchX = x
                    lastTouchY = y
                    scrollByInternal(dx.roundToInt(), ViewCompat.TYPE_TOUCH)
                } else {
                    val tx = x - initialTouchX
                    val ty = y - initialTouchY

                    lastTouchX = x
                    lastTouchY = y

                    if (abs(tx) > touchSlop && abs(tx) * 0.5 > abs(ty)) {
                        isBeingDragged = true
                        requestDisallowInterceptTouchEvent(true)
                        setScrollState(SCROLL_STATE_DRAGGING)
                    } else if (abs(ty) > touchSlop) {
                        isUnableToDrag = true
                    }
                }


            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                val xvel: Float = -(velocityTracker?.xVelocity ?: 0.0F)
                fling(xvel)
            }
            MotionEvent.ACTION_CANCEL -> {
                releaseDrag()
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fixedWidth = (fixedMinWidth + currentDragWith).coerceAtMost(fixedMaxWidth)
        val fixedWidthMeasureSpec = MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY)
        val fixedViewLayoutParams = fixedView.layoutParams as LayoutParams
        val fixedHeightMeasureSpec = getChildMeasureSpec(
            heightMeasureSpec,
            paddingTop + paddingBottom
                    + fixedViewLayoutParams.topMargin + fixedViewLayoutParams.bottomMargin,
            fixedViewLayoutParams.height
        )
        fixedView.measure(fixedWidthMeasureSpec, fixedHeightMeasureSpec)

        val scrollableViewLayoutParams = scrollableView.layoutParams as LayoutParams
        val scrollableWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val scrollableHeightMeasureSpec = getChildMeasureSpec(
            heightMeasureSpec,
            paddingTop + paddingBottom
                    + scrollableViewLayoutParams.topMargin + scrollableViewLayoutParams.bottomMargin,
            scrollableViewLayoutParams.height
        )
        scrollableView.measure(scrollableWidthMeasureSpec, scrollableHeightMeasureSpec)


        var maxWidth = fixedView.measuredWidth +
                fixedViewLayoutParams.leftMargin +
                fixedViewLayoutParams.rightMargin +
                scrollableView.measuredWidth +
                scrollableViewLayoutParams.leftMargin +
                scrollableViewLayoutParams.rightMargin +
                paddingLeft + paddingRight

        var maxHeight = (fixedView.measuredHeight
                + fixedViewLayoutParams.topMargin
                + fixedViewLayoutParams.bottomMargin).coerceAtLeast(
            scrollableView.measuredHeight
                    + scrollableViewLayoutParams.topMargin
                    + scrollableViewLayoutParams.bottomMargin
        ) + paddingTop + paddingBottom


        maxWidth = maxWidth.coerceAtLeast(suggestedMinimumWidth)
        maxHeight = maxHeight.coerceAtLeast(suggestedMinimumHeight)

        setMeasuredDimension(
            resolveSize(maxWidth, widthMeasureSpec),
            resolveSize(maxHeight, heightMeasureSpec)
        )

        scrollableMaxWidth = scrollableView.measuredWidth -
                (measuredWidth - (paddingLeft +
                        fixedViewLayoutParams.leftMargin +
                        fixedView.measuredWidth +
                        fixedViewLayoutParams.rightMargin +
                        scrollableViewLayoutParams.leftMargin)) +
                paddingRight + scrollableViewLayoutParams.rightMargin

        scrollableMaxWidth = scrollableMaxWidth.coerceAtLeast(0)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutFixedView()
        layoutScrollableView()
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun scrollByInternal(dx: Int, type: Int) {
        /*if (currentDragWith != 0) {
            if (currentDragWith shr 31 == dx shr 31) {
                if (dx > 0 && type == ViewCompat.TYPE_TOUCH) {
                    currentDragWith += (dx * BOUNCE_RATIO).toInt()
                }
            } else {
                if (dx > 0) {

                } else {

                }
            }
        }*/


        if (scrollableView.scrollX == 0) {
            if (dx > 0) {
                if (type == ViewCompat.TYPE_TOUCH) {
                    currentDragWith += (dx * BOUNCE_RATIO).toInt()
                }
            } else {
                if (currentDragWith > 0) {
                    var unConsumedDx = dx
                    if ((unConsumedDx * BOUNCE_RATIO).toInt() + currentDragWith < 0) {
                        val consumedDx = ((-currentDragWith) / BOUNCE_RATIO).toInt()
                        unConsumedDx -= consumedDx
                        currentDragWith = 0
                        scrollableView.scrollBy(-unConsumedDx, 0)
                    } else {
                        if (type == ViewCompat.TYPE_TOUCH) {
                            currentDragWith += (unConsumedDx * BOUNCE_RATIO).toInt()
                        }
                    }
                } else {
                    val currentScrollX = scrollableView.scrollX
                    val scrollX = (currentScrollX - dx).coerceAtMost(scrollableMaxWidth)
                    scrollableView.scrollTo(scrollX, 0)
                }
            }
        } else if (scrollableView.scrollX > 0) {
            if (dx > 0) {
                var unConsumedDx = dx
                val currentScrollX = scrollableView.scrollX
                val consumedDx = currentScrollX.coerceAtMost(unConsumedDx)
                unConsumedDx -= consumedDx
                scrollableView.scrollBy(-consumedDx, 0)
                if (type == ViewCompat.TYPE_TOUCH) {
                    currentDragWith += (unConsumedDx * BOUNCE_RATIO).toInt()
                } else {
                    if (currentDragWith + (unConsumedDx * BOUNCE_RATIO).toInt() <= 0) {
                        currentDragWith = 0
                    } else {
                        currentDragWith += (unConsumedDx * BOUNCE_RATIO).toInt()
                    }
                }
            } else {
                if (currentDragWith > 0) {
                    var unConsumedDx = dx
                    if ((unConsumedDx * BOUNCE_RATIO).toInt() + currentDragWith < 0) {
                        val consumedDx = ((-currentDragWith) / BOUNCE_RATIO).toInt()
                        unConsumedDx -= consumedDx
                        currentDragWith = 0
                        scrollableView.scrollBy(-unConsumedDx, 0)
                    } else {
                        if (type == ViewCompat.TYPE_TOUCH) {
                            currentDragWith += (unConsumedDx * BOUNCE_RATIO).toInt()
                        } else {
                            if (currentDragWith + (unConsumedDx * BOUNCE_RATIO).toInt() <= 0) {
                                currentDragWith = 0
                            } else {
                                currentDragWith += (unConsumedDx * BOUNCE_RATIO).toInt()
                            }
                        }
                    }
                } else {
                    val currentScrollX = scrollableView.scrollX
                    val scrollX = (currentScrollX - dx).coerceAtMost(scrollableMaxWidth)
                    scrollableView.scrollTo(scrollX, 0)
                }
            }
        }
    }

    fun releaseDrag() {
        if (currentDragWith == 0) return
        releaseDragHelper.start()
    }

    private fun fling(velocityX: Float) {
        /*if (currentDragWith != 0) {
            releaseDrag()
            return
        }*/
        if (abs(velocityX) < minimumVelocity) {
            releaseDrag()
            return
        }
        viewFlinger.fling(max(-maximumVelocity, min(velocityX.toInt(), maximumVelocity)))
    }

    private fun setScrollState(state: Int) {
        if (state == scrollState) {
            return
        }
        scrollState = state
        if (state != RecyclerView.SCROLL_STATE_SETTLING) {
            stopScrollersInternal()
        }
        dispatchOnScrollStateChanged(state)
    }

    private fun stopScrollersInternal() {
        //mViewFlinger.stop()
    }

    private fun dispatchOnScrollStateChanged(state: Int) {
        if (scrollListeners != null) {
            for (i in scrollListeners.indices.reversed()) {
                scrollListeners[i].onScrollStateChanged(this, state)
            }
        }
    }

    private fun pointView(view: View, event: MotionEvent): Boolean {
        return false
    }

    private fun layoutFixedView() {
        val layoutParams = fixedView.layoutParams as LayoutParams
        val verticalGravity = if (layoutParams.gravity == -1) {
            Gravity.TOP
        } else {
            layoutParams.gravity and Gravity.VERTICAL_GRAVITY_MASK
        }
        val left = paddingLeft + layoutParams.leftMargin
        val top = when (verticalGravity) {
            Gravity.CENTER_VERTICAL -> {
                paddingTop + layoutParams.topMargin +
                        ((measuredHeight -
                                (paddingTop + paddingBottom) -
                                (layoutParams.topMargin + layoutParams.bottomMargin) -
                                fixedView.measuredHeight) / 2)

            }
            Gravity.BOTTOM -> {
                measuredHeight - paddingBottom - layoutParams.bottomMargin - fixedView.measuredHeight
            }
            else -> {
                0
            }
        }
        val right = left + fixedView.measuredWidth
        val bottom = top + fixedView.measuredHeight
        fixedView.layout(left, top, right, bottom)
    }

    private fun layoutScrollableView() {
        val layoutParams = scrollableView.layoutParams as LayoutParams
        val verticalGravity = if (layoutParams.gravity == -1) {
            Gravity.TOP
        } else {
            layoutParams.gravity and Gravity.VERTICAL_GRAVITY_MASK
        }
        val left = fixedView.right + layoutParams.leftMargin
        val top = when (verticalGravity) {
            Gravity.CENTER_VERTICAL -> {
                paddingTop + layoutParams.topMargin +
                        ((measuredHeight -
                                (paddingTop + paddingBottom) -
                                (layoutParams.topMargin + layoutParams.bottomMargin) -
                                scrollableView.measuredHeight) / 2)

            }
            Gravity.BOTTOM -> {
                measuredHeight - paddingBottom - layoutParams.bottomMargin - scrollableView.measuredHeight
            }
            else -> {
                0
            }
        }
        val right = left + scrollableView.measuredWidth
        val bottom = top + scrollableView.measuredHeight
        scrollableView.layout(left, top, right, bottom)
    }

    class LayoutParams : MarginLayoutParams {

        companion object {
            private const val UNSPECIFIED_GRAVITY = -1
        }

        var gravity = UNSPECIFIED_GRAVITY

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs) {
            val a: TypedArray =
                c.obtainStyledAttributes(attrs, R.styleable.HorizontalScrollLayout_Layout)
            gravity = a.getInt(
                R.styleable.HorizontalScrollLayout_Layout_android_layout_gravity,
                UNSPECIFIED_GRAVITY
            )
            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(width: Int, height: Int, gravity: Int) : super(width, height) {
            this.gravity = gravity
        }

        constructor(layoutParams: LayoutParams) : super(layoutParams) {
            this.gravity = layoutParams.gravity
        }

        constructor(layoutParams: ViewGroup.LayoutParams) : super(layoutParams)

    }

    inner class ReleaseDragHelper : ValueAnimator.AnimatorUpdateListener {

        fun start() {
            val animator = ValueAnimator.ofInt(currentDragWith, 0).setDuration(300)
            animator.addUpdateListener(this)
            animator.start()
        }

        override fun onAnimationUpdate(animation: ValueAnimator) {
            currentDragWith = animation.animatedValue as Int
        }

    }

    inner class ViewFlinger : Runnable {

        private var lastFlingX = 0
        private val scroller: OverScroller by lazy {
            OverScroller(context, quinticInterpolator)
        }

        // When set to true, postOnAnimation callbacks are delayed until the run method completes
        private var eatRunOnAnimationRequest = false

        // Tracks if postAnimationCallback should be re-attached when it is done
        private var reSchedulePostAnimationCallback = false


        override fun run() {
            disableRunOnAnimationRequests()

            if (scroller.computeScrollOffset()) {
                val x: Int = scroller.currX
                val dx: Int = x - lastFlingX
                lastFlingX = x
                scrollByInternal(dx, ViewCompat.TYPE_NON_TOUCH)
                if (!awakenScrollBars()) {
                    invalidate()
                }
                if (!scroller.isFinished) {
                    postOnAnimation()
                } else {
                    releaseDrag()
                }
            } else {
                releaseDrag()
            }

            enableRunOnAnimationRequests()
        }


        private fun disableRunOnAnimationRequests() {
            reSchedulePostAnimationCallback = false
            eatRunOnAnimationRequest = true
        }

        private fun enableRunOnAnimationRequests() {
            eatRunOnAnimationRequest = false
            if (reSchedulePostAnimationCallback) {
                postOnAnimation()
            }
        }

        fun postOnAnimation() {
            if (eatRunOnAnimationRequest) {
                reSchedulePostAnimationCallback = true
            } else {
                removeCallbacks(this)
                ViewCompat.postOnAnimation(this@OldHorizontalScrollLayout, this)
            }
        }

        fun fling(velocityX: Int) {
            setScrollState(SCROLL_STATE_SETTLING)
            lastFlingX = 0
            scroller.fling(
                0,
                0,
                -velocityX,
                0,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Int.MAX_VALUE
            )
            postOnAnimation()
        }


        fun stop() {
            removeCallbacks(this)
            scroller.abortAnimation()
        }
    }

    interface OnScrollListener {

        fun onScrollStateChanged(view: OldHorizontalScrollLayout, newState: Int) {}

    }
}