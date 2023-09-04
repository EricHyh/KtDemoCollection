package com.hyh.paging3demo.widget.horizontal.internal

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 *
 *
 * @author eriche 2023/9/1
 */
class SyncHorizontalScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

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
                removeOnScrollListener(onScrollListener)
            } else {
                removeOnScrollListener(onScrollListener)
                addOnScrollListener(onScrollListener)
            }
        }
    }

    init {
        addOnScrollListener(onScrollStateListener)
    }

    data class OnScrolledData(
        val recyclerView: RecyclerView,
        var dx: Int,
        var dy: Int
    )
}