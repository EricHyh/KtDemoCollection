package com.hyh.paging3demo.widget.horizontal.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
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
abstract class BaseHorizontalScrollLayout : CoordinatorLayout {
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
    private lateinit var scrollable: Scrollable<IScrollingData, IScrolledData>

    private lateinit var fixedBehavior: FixedBehavior
    private lateinit var scrollableBehavior: ScrollableBehavior

    private var initialized: Boolean = false


    private var helper: HorizontalScrollSyncHelper? = null

    private var _scrollState: ScrollState? = null
    val scrollState: ScrollState?
        get() = _scrollState

    private var pendingIdleScrolledData: IScrolledData? = null

    private val onScrollableScrollListener = object : OnScrollableScrollListener {

        override fun onDragging(currentDragWith: Int) {
            helper?.notifyDragging(currentDragWith)
        }

        override fun onRebounding(currentDragWith: Int) {
            helper?.notifyRebounding(currentDragWith)
        }

        override fun onScrolling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
            helper?.notifyScrolling(scrollingData, scrolledData)
        }

        override fun onSettling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
            helper?.notifySettling(scrollingData, scrolledData)
        }

        override fun onIdle(scrolledData: IScrolledData) {
            helper?.notifyIdle(scrolledData)
        }

        override fun onInitial() {
            helper?.notifyInitial()
        }
    }

    private val scrollSyncObserver = object : ScrollSyncObserver() {

        override fun onDragging(currentDragWith: Int) {
            scrollable.resetScroll()
            fixedBehavior.currentDragWith = currentDragWith
            _scrollState = ScrollState.DRAG
        }

        override fun onRebounding(currentDragWith: Int) {
            scrollable.resetScroll()
            fixedBehavior.currentDragWith = currentDragWith
            _scrollState = ScrollState.REBOUND
        }

        override fun onScrolling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
            scrollable.scroll(ScrollState.SCROLL, scrollingData)
            fixedBehavior.currentDragWith = 0
            _scrollState = ScrollState.SCROLL
        }

        override fun onSettling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
            scrollable.scroll(ScrollState.SETTLING, scrollingData)
            fixedBehavior.currentDragWith = 0
            _scrollState = ScrollState.SETTLING
        }

        override fun onIdle(scrolledData: IScrolledData) {
            scrollable.scrollTo(scrolledData)

            val pendingIdleScrolledData = this@BaseHorizontalScrollLayout.pendingIdleScrolledData
            this@BaseHorizontalScrollLayout.pendingIdleScrolledData = null
            if (pendingIdleScrolledData != null) {
                this@BaseHorizontalScrollLayout.helper?.notifyIdle(scrolledData)
            }

            fixedBehavior.currentDragWith = 0
            _scrollState = ScrollState.IDLE
        }

        override fun onInitial() {
            scrollable.resetScroll()
            fixedBehavior.currentDragWith = 0
            _scrollState = ScrollState.INITIAL
        }

        override fun onScrolled(scrollState: ScrollState, scrolledData: IScrolledData) {
            scrollable.scrollTo(scrolledData)
            fixedBehavior.currentDragWith = 0
            _scrollState = scrollState
        }

        override fun onStopScroll() {
            fixedBehavior.stopScroll()
        }
    }

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
        scrollable = asScrollable(scrollableView) as Scrollable<IScrollingData, IScrolledData>
        val fixedViewLayoutParams = fixedView.layoutParams as LayoutParams
        fixedViewLayoutParams.behavior = FixedBehavior(
            fixedMinWidth,
            fixedMaxWidth,
            scrollableView,
            scrollable,
            onScrollableScrollListener = onScrollableScrollListener,
            isAllowReleaseDrag = { helper?.isAllowReleaseDrag(scrollSyncObserver) ?: true }
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

    protected fun notifyScrollingEvent(
        scrollingData: IScrollingData,
        scrolledData: IScrolledData,
    ) {
        helper?.notifyScrolling(scrollingData, scrolledData)
    }

    protected fun notifySettlingEvent(
        scrollingData: IScrollingData,
        scrolledData: IScrolledData,
    ) {
        helper?.notifySettling(scrollingData, scrolledData)
    }

    fun bindHorizontalScrollSyncHelper(helper: HorizontalScrollSyncHelper) {
        val oldHelper = this.helper
        this.helper = helper
        oldHelper?.removeObserver(scrollSyncObserver)
        if (isAttachedToWindow) {
            this.helper?.addObserver(scrollSyncObserver)
        }
    }

    protected fun syncScroll() {
        this.helper?.sync(scrollSyncObserver)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                helper?.notifyActionDown(scrollSyncObserver)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                helper?.notifyActionCancel(scrollSyncObserver)
                if (helper?.isAllowReleaseDrag(scrollSyncObserver) != false) {
                    fixedBehavior.tryRebound()
                }
            }
        }
        val result = super.dispatchTouchEvent(ev) || fixedBehavior.currentDragWith != 0
        if (!result && action == MotionEvent.ACTION_DOWN) {
            helper?.notifyActionCancel(scrollSyncObserver)
            if (helper?.isAllowReleaseDrag(scrollSyncObserver) != false) {
                fixedBehavior.tryRebound()
            }
        }
        return result
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.helper?.addObserver(scrollSyncObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this.helper?.removeObserver(scrollSyncObserver)
        scrollable.stopScroll()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig ?: return
        newConfig.orientation
        newConfig.screenLayout
        newConfig.screenWidthDp
    }

    protected abstract fun findFixedView(): View

    protected abstract fun findScrollableView(): View

    protected abstract fun asScrollable(scrollableView: View): Scrollable<*, *>

    fun scrollTo(data: IScrolledData) {
        if (_scrollState == null
            || _scrollState == ScrollState.INITIAL
            || _scrollState == ScrollState.IDLE
        ) {
            this.helper?.notifyIdle(data)
        } else {
            this.pendingIdleScrolledData = data
        }
    }
}