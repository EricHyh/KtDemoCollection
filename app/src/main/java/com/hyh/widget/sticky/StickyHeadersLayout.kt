package com.hyh.widget.sticky

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.util.contains
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/11/30
 */
class StickyHeadersLayout : FrameLayout {

    private val mObserver = RecyclerViewDataObserver()
    private var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    private var mVisibleItemFinder: VisibleItemFinder = DefaultVisibleItemFinder()
    private var mRecyclerView: RecyclerView? = null
    private var mStickyHeadersAdapter: IStickyHeadersAdapter<RecyclerView.ViewHolder>? = null

    //包括不可见与可见的
    private val mHeaderPositions: TreeSet<Int> = TreeSet()

    private var mLastHeaderPosition = RecyclerView.NO_POSITION

    private val mAttachedHeaders: SparseArray<StickyHeader> = SparseArray()

    private var mInitialTouchX: Int = 0
    private var mInitialTouchY: Int = 0
    private var mLastTouchX: Int = 0
    private var mLastTouchY: Int = 0
    private val mTouchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    private val mOnScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            registerAdapterDataObserver(recyclerView)
            onScrolled(recyclerView, 0, 0)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val firstCompletelyVisibleItemPosition =
                mVisibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
            //val position = mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)

            val currentHeaderPosition = findCurrentHeaderPosition(recyclerView)
            if (currentHeaderPosition < 0) {
                detachStickyHeader(mLastHeaderPosition)
                mLastHeaderPosition = RecyclerView.NO_POSITION
            } else {
                if (mLastHeaderPosition != currentHeaderPosition) {
                    if (mLastHeaderPosition >= 0) {
                        bindNormalViewHolder(recyclerView, mLastHeaderPosition)
                    }
                    detachStickyHeader(mLastHeaderPosition)
                    mLastHeaderPosition = currentHeaderPosition
                }
                var header: StickyHeader? = getStickyHeader(currentHeaderPosition)
                if (header == null) {
                    header =
                        createStickyHeader(recyclerView, currentHeaderPosition) ?: return
                    bindHeaderViewHolder(header, currentHeaderPosition)
                    attachStickyHeader(currentHeaderPosition, header)
                    header.viewHolder.itemView.measure(0, 0)
                }

                val firstVisibleItemPosition =
                    mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
                if (firstVisibleItemPosition == currentHeaderPosition) {
                    if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                        bindNormalViewHolder(recyclerView, currentHeaderPosition)
                    }
                }
                if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    bindHeaderViewHolder(header, currentHeaderPosition)
                }


                val headerHeight = header.viewHolder.itemView.measuredHeight

                val nextHeaderPosition = findNextHeaderPosition(recyclerView, currentHeaderPosition)
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(nextHeaderPosition)

                if (viewHolder != null && viewHolder.itemView.top < headerHeight) {
                    val headerOffsetY = viewHolder.itemView.top - headerHeight
                    header.viewHolder.itemView.translationY = headerOffsetY.toFloat()
                } else {
                    header.viewHolder.itemView.translationY = 0.0f
                }
            }
        }
    }

    private fun bindNormalViewHolder(recyclerView: RecyclerView, position: Int) {
        val stickyHeader = getStickyHeader(position) ?: return
        if (stickyHeader.bound) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder != null) {
                Log.d("StickyHeadersLayout", "bindNormalViewHolder -> $position")
                mAdapter?.onBindViewHolder(viewHolder, position)
                stickyHeader.bound = false
            }
        }
    }

    private fun bindHeaderViewHolder(header: StickyHeader, position: Int) {
        if (header.bound) return
        Log.d("StickyHeadersLayout", "bindHeaderViewHolder -> $position")
        mStickyHeadersAdapter?.onBindStickyViewHolder(header.viewHolder, position)
        header.bound = true
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Suppress("UNCHECKED_CAST")
    fun setup(recyclerView: RecyclerView, adapter: IStickyHeadersAdapter<*>) {
        this.mRecyclerView = recyclerView
        this.mStickyHeadersAdapter = adapter as IStickyHeadersAdapter<RecyclerView.ViewHolder>
        recyclerView.addOnScrollListener(mOnScrollListener)
        registerAdapterDataObserver(recyclerView)
    }


    private var mScrollVertically = false

    /*override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked


        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mInitialTouchX = ev.x.roundToInt()
                mInitialTouchY = ev.y.roundToInt()
                mLastTouchX = mInitialTouchX
                mLastTouchY = mInitialTouchY
            }
            MotionEvent.ACTION_MOVE -> {

                if (mScrollVertically) {
                    return false
                }

                val curX = ev.x.roundToInt()
                val curY = ev.y.roundToInt()

                val tx = abs(curX - mInitialTouchX)
                val ty = abs(curY - mInitialTouchY)

                if (tx > mTouchSlop || ty > mTouchSlop) {
                    if (ty > tx) {
                        mScrollVertically = true
                        return false
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mScrollVertically = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }*/


    override fun canScrollVertically(direction: Int): Boolean {
        return super.canScrollVertically(direction) || mRecyclerView?.canScrollVertically(direction) ?: false
    }


    //region private
    private fun getStickyHeader(position: Int): StickyHeader? {
        return mAttachedHeaders.get(position)
    }

    private fun createStickyHeader(
        recyclerView: RecyclerView,
        position: Int
    ): StickyHeader? {
        val adapter = recyclerView.adapter ?: return null
        val itemViewType = adapter.getItemViewType(position)
        val viewHolder: RecyclerView.ViewHolder =
            recyclerView.recycledViewPool.getRecycledView(itemViewType)
                ?: adapter.onCreateViewHolder(recyclerView, itemViewType)
                ?: return null
        return StickyHeader(viewHolder, position)
    }

    private fun attachStickyHeader(position: Int, header: StickyHeader) {
        if (!mAttachedHeaders.contains(position)) {
            mAttachedHeaders.put(position, header)
            addView(header.viewHolder.itemView)
        }
    }

    private fun detachStickyHeader(position: Int) {
        val headerHolder = mAttachedHeaders.get(position)
        if (headerHolder != null) {
            mAttachedHeaders.remove(position)
            removeView(headerHolder.viewHolder.itemView)
        }
    }

    private fun isHeaderAttached(position: Int): Boolean {
        return mAttachedHeaders.contains(position)
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return mStickyHeadersAdapter?.isStickyHeader(position) ?: false
    }

    private fun findCurrentHeaderPosition(recyclerView: RecyclerView): Int {
        val firstCompletelyVisibleItemPosition =
            mVisibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
        if (firstCompletelyVisibleItemPosition == 0) return RecyclerView.NO_POSITION
        for (position in (firstCompletelyVisibleItemPosition - 1) downTo 0) {
            if (isHeaderPosition(position)) return position
        }
        return RecyclerView.NO_POSITION
    }

    private fun findLastHeaderPosition(recyclerView: RecyclerView): Int {
        val firstVisibleItemPosition =
            mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        if (firstVisibleItemPosition <= 0) {
            return RecyclerView.NO_POSITION
        }
        return RecyclerView.NO_POSITION
    }

    private fun findNextHeaderPosition(recyclerView: RecyclerView, lastHeaderPosition: Int): Int {
        val firstVisibleItemPosition =
            mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        val lastVisibleItemPosition =
            mVisibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
        return if (firstVisibleItemPosition == lastHeaderPosition) {
            findFirstHeaderPosition(firstVisibleItemPosition + 1, lastVisibleItemPosition)
        } else if (firstVisibleItemPosition > lastHeaderPosition) {
            findFirstHeaderPosition(firstVisibleItemPosition, lastVisibleItemPosition)
        } else {
            findFirstHeaderPosition(lastHeaderPosition, lastVisibleItemPosition)
        }
    }

    private fun findFirstHeaderPosition(startPosition: Int, endPosition: Int): Int {
        if (startPosition > endPosition) return RecyclerView.NO_POSITION
        return mStickyHeadersAdapter?.let {
            for (position in startPosition..endPosition) {
                if (it.isStickyHeader(position)) return@let position
            }
            RecyclerView.NO_POSITION
        } ?: RecyclerView.NO_POSITION
    }

    private fun registerAdapterDataObserver(recyclerView: RecyclerView) {
        (recyclerView.parent as ViewGroup).isMotionEventSplittingEnabled = false
        val adapter = recyclerView.adapter
        if (adapter == mAdapter) return
        if (mAdapter != null) {
            mAdapter?.unregisterAdapterDataObserver(mObserver)
        }
        mAdapter = adapter
        mObserver.onChanged()
        adapter?.registerAdapterDataObserver(mObserver)
    }
    //endregion

    private inner class RecyclerViewDataObserver : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            super.onChanged()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
        }

        override fun onStateRestorationPolicyChanged() {
            super.onStateRestorationPolicyChanged()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
        }
    }

    private class StickyHeader(
        val viewHolder: RecyclerView.ViewHolder,
        val position: Int
    ) {

        var bound: Boolean = false

    }
}