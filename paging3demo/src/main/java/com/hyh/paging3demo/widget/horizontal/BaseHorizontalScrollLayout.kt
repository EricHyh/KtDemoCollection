package com.hyh.paging3demo.widget.horizontal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/28
 */
abstract class BaseHorizontalScrollLayout : CoordinatorLayout, ScrollSyncObserver {
    companion object {
        private const val TAG = "HorizontalScrollLayout"
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private lateinit var fixedView: View
    private lateinit var scrollableView: View
    private lateinit var scrollable: Scrollable<Any>

    private var initialized: Boolean = false


    private var helper: HorizontalScrollSyncHelper? = null

    protected fun initView() {
        fixedView = findFixedView()
        scrollableView = findFixedView()
        scrollable = asScrollable(scrollableView)
        scrollable.setOnScrollChangeListener(object : Scrollable.OnScrollChangedListener {
            override fun onScrollChanged() {

            }
        })
        initialized = true
    }


    fun bindHorizontalScrollSyncHelper(helper: HorizontalScrollSyncHelper) {
        val oldHelper = this.helper
        this.helper = helper
        oldHelper?.removeObserver(this)
        this.helper?.addObserver(this)
    }

    override fun onScroll(data: Any, scrollState: ScrollState) {
        if (initialized) {
            scrollable.scrollTo(data)
        }
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

    protected abstract fun asScrollable(scrollableView: View): Scrollable<Any>

}


