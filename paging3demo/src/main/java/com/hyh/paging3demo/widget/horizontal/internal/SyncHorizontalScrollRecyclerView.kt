package com.hyh.paging3demo.widget.horizontal.internal

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

/**
 * TODO
 *
 * @author eriche 2023/9/1
 */
class SyncHorizontalScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    private val TAG = "SyncHorizontalScrollTT"

    private val onScrolledData = OnScrolledData(
        this,
        0,
        0
    )

    var scrollListener: ((OnScrolledData) -> Unit)? = null

    private val onScrollListener: OnScrollListener = object : OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            scrollListener?.invoke(onScrolledData.also {
                it.dx = dx
                it.dy = dy
            })
        }
    }

    private val onScrollStateListener: OnScrollListener = object : OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == SCROLL_STATE_IDLE) {
                Log.d(TAG, "removeOnScrollListener: ${this@SyncHorizontalScrollRecyclerView}")
                removeOnScrollListener(onScrollListener)
            } else {
                Log.d(TAG, "addOnScrollListener: ${this@SyncHorizontalScrollRecyclerView}")
                removeOnScrollListener(onScrollListener)
                addOnScrollListener(onScrollListener)
            }
        }
    }

    init {
        addOnScrollListener(onScrollStateListener)
    }


//    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        return super.dispatchTouchEvent(ev)
//    }
//
//    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
//        return super.onInterceptTouchEvent(e)
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(ev: MotionEvent): Boolean {
//        when (ev.actionMasked) {
//            MotionEvent.ACTION_DOWN -> {
//                removeOnScrollListener(onScrollListener)
//                addOnScrollListener(onScrollListener)
//            }
//            MotionEvent.ACTION_POINTER_DOWN -> {}
//            MotionEvent.ACTION_UP -> {}
//            MotionEvent.ACTION_POINTER_UP -> {}
//            MotionEvent.ACTION_CANCEL -> {}
//        }
//        return super.onTouchEvent(ev)
//    }

    data class OnScrolledData(
        val recyclerView: RecyclerView,
        var dx: Int,
        var dy: Int
    )
}