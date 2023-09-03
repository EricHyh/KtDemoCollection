package com.hyh.paging3demo.widget.horizontal.internal

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.widget.horizontal.HorizontalScrollSyncHelper
import com.hyh.paging3demo.widget.horizontal.ScrollState
import com.hyh.paging3demo.widget.horizontal.ScrollSyncObserver

/**
 * 支持水平滑动的控件基类，分为固定部分与可滑动部分
 *
 * @author eriche 2021/12/28
 */
abstract class BaseHorizontalScrollLayout : CoordinatorLayout, ScrollSyncObserver {
    companion object {
        private const val TAG = "HorizontalScrollLayout"
    }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    var fixedMinWidth: Int = 0
        set(value) {
            if (field == value) return
            field = value
            if (!initialized) return
            fixedBehavior.fixedMinWidth = value
            scrollableBehavior.fixedMinWidth = value
            requestLayout()
        }

    var fixedMaxWidth: Int = Int.MAX_VALUE
        set(value) {
            if (field == value) return
            field = value
            if (!initialized) return
            fixedBehavior.fixedMaxWidth = value
            requestLayout()
        }

    private lateinit var fixedView: View
    lateinit var scrollableView: View
    private lateinit var scrollable: Scrollable<IScrollData>

    private lateinit var fixedBehavior: FixedBehavior
    private lateinit var scrollableBehavior: ScrollableBehavior

    private var initialized: Boolean = false


    private var helper: HorizontalScrollSyncHelper? = null

    private var _scrollState: ScrollState? = null
    val scrollState: ScrollState?
        get() = _scrollState

    private var pendingIdleScrollData: IScrollData? = null

    @SuppressLint("CustomViewStyleable")
    private fun init(attrs: AttributeSet?, defStyle: Int = 0) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.HorizontalScrollLayout,
                defStyle,
                0
            )
            fixedMinWidth = typedArray.getDimensionPixelSize(
                R.styleable.HorizontalScrollLayout_fixed_min_width,
                fixedMinWidth
            )
            fixedMaxWidth = typedArray.getDimensionPixelSize(
                R.styleable.HorizontalScrollLayout_fixed_max_width,
                fixedMaxWidth
            )
            typedArray.recycle()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun initView() {
        fixedView = findFixedView()
        scrollableView = findScrollableView()
        scrollable = asScrollable(scrollableView) as Scrollable<IScrollData>
        val fixedViewLayoutParams = fixedView.layoutParams as LayoutParams
        fixedViewLayoutParams.behavior = FixedBehavior(
            fixedMinWidth,
            fixedMaxWidth,
            scrollableView,
            scrollable,
            onScrollChanged = { scrollState: ScrollState, data: Any ->
                helper?.notifyScrollEvent(scrollState, data)
            },
            isAllowReleaseDrag = { helper?.isAllowReleaseDrag(this) ?: true }
        ).apply {
            fixedBehavior = this
        }

        val scrollableViewLayoutParams = scrollableView.layoutParams as LayoutParams
        scrollableViewLayoutParams.behavior = ScrollableBehavior(fixedMinWidth).apply {
            scrollableBehavior = this
        }

        initialized = true

        requestLayout()
    }

    protected fun notifyScrollEvent(
        scrollState: ScrollState,
        data: Any
    ) {
        helper?.notifyScrollEvent(scrollState, data)
    }

    fun bindHorizontalScrollSyncHelper(helper: HorizontalScrollSyncHelper) {
        val oldHelper = this.helper
        this.helper = helper
        oldHelper?.removeObserver(this)
        if (isAttachedToWindow) {
            this.helper?.addObserver(this)
        }
    }

    protected fun syncScroll() {
        this.helper?.sync(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                helper?.notifyActionDown(this)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                helper?.notifyActionCancel(this)
                if (helper?.isAllowReleaseDrag(this) != false) {
                    fixedBehavior.tryRebound()
                }
            }
        }
        val result = super.dispatchTouchEvent(ev) || fixedBehavior.currentDragWith != 0
        if (!result && action == MotionEvent.ACTION_DOWN) {
            helper?.notifyActionCancel(this)
            if (helper?.isAllowReleaseDrag(this) != false) {
                fixedBehavior.tryRebound()
            }
        }
        return result
    }

    override fun onScroll(scrollState: ScrollState, data: Any) {
        Log.d(TAG, "onScroll: ${this.hashCode()}, $isAttachedToWindow, $initialized, $scrollState")
        if (!isAttachedToWindow) return
        if (initialized) {
            when (scrollState) {
                ScrollState.INITIAL -> {
                    scrollable.resetScroll()
                    fixedBehavior.currentDragWith = 0
                }
                ScrollState.IDLE -> {
                    scrollable.scrollTo(scrollState, data as IScrollData)

                    val pendingIdleScrollData = this.pendingIdleScrollData
                    this.pendingIdleScrollData = null
                    if (pendingIdleScrollData != null) {
                        this.helper?.notifyScrollEvent(ScrollState.IDLE, data)
                    }

                    fixedBehavior.currentDragWith = 0
                }
                ScrollState.SCROLL -> {
                    scrollable.scrollTo(scrollState, data as IScrollData)
                    fixedBehavior.currentDragWith = 0
                }
                ScrollState.DRAG -> {
                    scrollable.resetScroll()
                    fixedBehavior.currentDragWith = data as Int
                }
                ScrollState.REBOUND -> {
                    scrollable.resetScroll()
                    fixedBehavior.currentDragWith = data as Int
                }
                ScrollState.SETTLING -> {
                    scrollable.scrollTo(scrollState, data as IScrollData)
                    fixedBehavior.currentDragWith = 0
                }
            }
            this._scrollState = scrollState
        }
    }

    override fun onStopScroll() {
        fixedBehavior.stopScroll()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.helper?.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this.helper?.removeObserver(this)
        scrollable.stopScroll()
    }

    protected abstract fun findFixedView(): View

    protected abstract fun findScrollableView(): View

    protected abstract fun asScrollable(scrollableView: View): Scrollable<*>

    fun scrollTo(data: IScrollData) {
        if (_scrollState == null
            || _scrollState == ScrollState.INITIAL
            || _scrollState == ScrollState.IDLE
        ) {
            this.helper?.notifyScrollEvent(ScrollState.IDLE, data)
        } else {
            this.pendingIdleScrollData = data
        }
    }
}