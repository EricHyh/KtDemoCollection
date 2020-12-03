package com.hyh.widget.sticky

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
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

    private val mAttachedHeaders: SparseArray<RecyclerView.ViewHolder> = SparseArray()

    private var mBindStickyViewHolder = false

    private val mOnScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            registerAdapterDataObserver(recyclerView)
            onScrolled(recyclerView, 0, 0)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val firstCompletelyVisibleItemPosition = mVisibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
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
                var headerHolder = getHeaderHolder(currentHeaderPosition)
                if (headerHolder == null) {
                    headerHolder = createStickyViewHolder(recyclerView, currentHeaderPosition) ?: return
                    bindHeaderViewHolder(headerHolder, currentHeaderPosition)
                    attachStickyHeader(currentHeaderPosition, headerHolder)
                    headerHolder.itemView.measure(0, 0)
                }


                val firstVisibleItemPosition = mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
                if (firstVisibleItemPosition == currentHeaderPosition) {
                    if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                        bindNormalViewHolder(recyclerView, currentHeaderPosition)
                    } else {
                        bindHeaderViewHolder(headerHolder, currentHeaderPosition)
                    }
                }

                val headerHeight = headerHolder.itemView.measuredHeight

                val nextHeaderPosition = findNextHeaderPosition(recyclerView, currentHeaderPosition)
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(nextHeaderPosition)

                if (viewHolder != null && viewHolder.itemView.top < headerHeight) {
                    val headerOffsetY = viewHolder.itemView.top - headerHeight
                    headerHolder.itemView.translationY = headerOffsetY.toFloat()
                } else {
                    headerHolder.itemView.translationY = 0.0f
                }
            }
        }
    }

    private fun bindNormalViewHolder(recyclerView: RecyclerView, position: Int) {
        if (mBindStickyViewHolder) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder != null) {
                mAdapter?.onBindViewHolder(viewHolder, position)
            }
            mBindStickyViewHolder = false
        }
    }

    private fun bindHeaderViewHolder(headerHolder: RecyclerView.ViewHolder, position: Int) {
        mStickyHeadersAdapter?.onBindStickyViewHolder(headerHolder, position)
        mBindStickyViewHolder = true
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @Suppress("UNCHECKED_CAST")
    fun setup(recyclerView: RecyclerView, adapter: IStickyHeadersAdapter<*>) {
        this.mRecyclerView = recyclerView
        this.mStickyHeadersAdapter = adapter as IStickyHeadersAdapter<RecyclerView.ViewHolder>
        recyclerView.addOnScrollListener(mOnScrollListener)
        registerAdapterDataObserver(recyclerView)
    }

    //region private
    private fun getHeaderHolder(position: Int): RecyclerView.ViewHolder? {
        return mAttachedHeaders.get(position)
    }

    private fun createStickyViewHolder(recyclerView: RecyclerView, position: Int): RecyclerView.ViewHolder? {
        val adapter = recyclerView.adapter ?: return null
        val itemViewType = adapter.getItemViewType(position)
        return recyclerView.recycledViewPool.getRecycledView(itemViewType) ?: adapter.onCreateViewHolder(recyclerView, itemViewType)
    }

    private fun attachStickyHeader(position: Int, viewHolder: RecyclerView.ViewHolder) {
        if (!mAttachedHeaders.contains(position)) {
            mAttachedHeaders.put(position, viewHolder)
            addView(viewHolder.itemView)
        }
    }

    private fun detachStickyHeader(position: Int) {
        val headerHolder = mAttachedHeaders.get(position)
        if (headerHolder != null) {
            mAttachedHeaders.remove(position)
            removeView(headerHolder.itemView)
        }
    }

    private fun isHeaderAttached(position: Int): Boolean {
        return mAttachedHeaders.contains(position)
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return mStickyHeadersAdapter?.isStickyHeader(position) ?: false
    }

    private fun findCurrentHeaderPosition(recyclerView: RecyclerView): Int {
        val firstCompletelyVisibleItemPosition = mVisibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
        if (firstCompletelyVisibleItemPosition == 0) return RecyclerView.NO_POSITION
        for (position in (firstCompletelyVisibleItemPosition - 1) downTo 0) {
            if (isHeaderPosition(position)) return position
        }
        return RecyclerView.NO_POSITION
    }

    private fun findLastHeaderPosition(recyclerView: RecyclerView): Int {
        val firstVisibleItemPosition = mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        if (firstVisibleItemPosition <= 0) {
            return RecyclerView.NO_POSITION
        }
        return RecyclerView.NO_POSITION
    }

    private fun findNextHeaderPosition(recyclerView: RecyclerView, lastHeaderPosition: Int): Int {
        val firstVisibleItemPosition = mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        val lastVisibleItemPosition = mVisibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
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

    private class StickyHeader {

        var viewHolder: RecyclerView.ViewHolder? = null
        var position: Int? = null

    }

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
}