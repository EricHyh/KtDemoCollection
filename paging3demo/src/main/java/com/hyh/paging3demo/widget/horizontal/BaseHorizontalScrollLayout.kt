package com.hyh.paging3demo.widget.horizontal

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.hyh.paging3demo.R

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

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
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
    private lateinit var scrollableView: View
    private lateinit var scrollable: Scrollable<IScrollData>

    private lateinit var fixedBehavior: FixedBehavior
    private lateinit var scrollableBehavior: ScrollableBehavior

    private var initialized: Boolean = false


    private var helper: HorizontalScrollSyncHelper? = null

    @SuppressLint("CustomViewStyleable")
    private fun init(attrs: AttributeSet?, defStyle: Int = 0) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HorizontalScrollLayout, defStyle, 0)
            fixedMinWidth = typedArray.getDimensionPixelSize(R.styleable.HorizontalScrollLayout_fixed_min_width, fixedMinWidth)
            fixedMaxWidth = typedArray.getDimensionPixelSize(R.styleable.HorizontalScrollLayout_fixed_max_width, fixedMaxWidth)
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
            scrollable,
            onScrollChanged = { scrollState: ScrollState, data: Any ->
                helper?.notifyScrollEvent(scrollState, data)
            },
            onStopScroll = {
                helper?.notifyStopScroll()
            }
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
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                helper?.notifyStopScroll()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                fixedBehavior.tryRebound()
            }
        }
        return super.dispatchTouchEvent(ev) || fixedBehavior.currentDragWith != 0
    }

    override fun onScroll(scrollState: ScrollState, data: Any) {
        if (!isAttachedToWindow) return
        if (initialized) {
            when (scrollState) {
                ScrollState.INITIAL -> {
                    scrollable.resetScroll()
                    fixedBehavior.currentDragWith = 0
                }
                ScrollState.IDLE -> {
                    scrollable.scrollTo(data as IScrollData)
                    fixedBehavior.currentDragWith = 0
                }
                ScrollState.SCROLL -> {
                    scrollable.scrollTo(data as IScrollData)
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
                    scrollable.scrollTo(data as IScrollData)
                    fixedBehavior.currentDragWith = 0
                }
            }
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
    }

    protected abstract fun findFixedView(): View

    protected abstract fun findScrollableView(): View

    protected abstract fun asScrollable(scrollableView: View): Scrollable<*>

}


