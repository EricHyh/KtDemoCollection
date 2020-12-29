package com.hyh.widget.sticky

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/11/30
 */
class StickyHeadersLayout : FrameLayout {

    private val mObserver = RecyclerViewDataObserver()
    private var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    private val mDecorations: MutableList<StickyHeaderDecoration> = mutableListOf()

    private var mVisibleItemFinder: VisibleItemFinder = DefaultVisibleItemFinder()
    private var mRecyclerView: RecyclerView? = null
    private var mRecyclerViewState: RecyclerView.State? = null

    private var mStickyHeadersAdapter: IStickyHeadersAdapter<RecyclerView.ViewHolder>? = null

    private var mLastHeaderPosition = RecyclerView.NO_POSITION

    private val mAttachedHeaders: SparseArray<StickyHeader> = SparseArray()

    private var mInitialTouchX: Int = 0
    private var mInitialTouchY: Int = 0
    private var mLastTouchX: Int = 0
    private var mLastTouchY: Int = 0
    private val mTouchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    private var mScrollVertically = false
    private var mTouchHeaders = false
    private var mRedispatchTouchEvent = false

    private val mOnScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            registerAdapterDataObserver(recyclerView)
            updateStickyHeader(recyclerView)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            updateStickyHeader(recyclerView)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mRecyclerView?.postIdleTask {
            updateStickyHeader(this)
        }
    }

    private fun updateStickyHeader(
        recyclerView: RecyclerView,
        scrollState: Int = recyclerView.scrollState
    ) {
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
                header = createStickyHeader(recyclerView, currentHeaderPosition) ?: return
                bindHeaderViewHolder(header, currentHeaderPosition)
                attachStickyHeader(currentHeaderPosition, header)
                header.viewHolder.itemView.measure(0, 0)
            }

            val firstVisibleItemPosition =
                mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
            if (firstVisibleItemPosition == currentHeaderPosition) {
                if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                    bindNormalViewHolder(recyclerView, currentHeaderPosition)
                }
            }
            if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
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

            if (header.viewHolder.itemView.height < header.viewHolder.itemView.measuredHeight
                || header.viewHolder.itemView.width < this.width
            ) {
                header.viewHolder.itemView.requestLayout()
            }
        }
    }

    private fun bindNormalViewHolder(recyclerView: RecyclerView, position: Int) {
        val stickyHeader = getStickyHeader(position) ?: return
        if (stickyHeader.bound) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            if (viewHolder != null) {
                mAdapter?.onBindViewHolder(viewHolder, position)
                stickyHeader.bound = false
            }
        }
    }

    private fun bindHeaderViewHolder(header: StickyHeader, position: Int, force: Boolean = false) {
        if (!force && header.bound) return
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
        this.mRecyclerViewState = recyclerView.getRecyclerViewState()
        this.mStickyHeadersAdapter = adapter as IStickyHeadersAdapter<RecyclerView.ViewHolder>
        recyclerView.addOnScrollListener(mOnScrollListener)
        registerAdapterDataObserver(recyclerView)
    }

    fun addHeaderDecoration(decoration: StickyHeaderDecoration) {
        mDecorations.add(decoration)
        requestLayout()
    }

    fun removeHeaderDecoration(decoration: StickyHeaderDecoration) {
        mDecorations.remove(decoration)
        requestLayout()
    }

    fun setItem() {
        //mRecyclerView.getItemDecorationCount()
    }

    fun getHeaderAdapterPosition(headerView: View): Int {
        return -1
    }

    public override fun onDraw(c: Canvas) {
        super.onDraw(c)

        val count = mDecorations.size
        for (i in 0 until count) {
            mDecorations.get(i).onDraw(c, this)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun measureChild(
        child: View?,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (mRedispatchTouchEvent) {
                    mRedispatchTouchEvent = false
                    mScrollVertically = false
                    return false
                }
                mInitialTouchX = ev.x.roundToInt()
                mInitialTouchY = ev.y.roundToInt()
                mLastTouchX = mInitialTouchX
                mLastTouchY = mInitialTouchY
                mTouchHeaders = mAttachedHeaders.size() > 0 && isTouchHeaders(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mTouchHeaders) {
                    return super.dispatchTouchEvent(ev)
                }
                if (mScrollVertically) {
                    return false
                }
                val curX = ev.x.roundToInt()
                val curY = ev.y.roundToInt()

                val tx = abs(curX - mInitialTouchX)
                val ty = abs(curY - mInitialTouchY)

                if (tx > mTouchSlop || ty > mTouchSlop) {
                    if (ty > tx) {
                        return findParent()?.let {
                            mScrollVertically = true
                            val newEvent = MotionEvent.obtain(ev)
                            newEvent.action = MotionEvent.ACTION_DOWN
                            mRedispatchTouchEvent = true
                            it.dispatchTouchEvent(newEvent)
                            false
                        } ?: super.dispatchTouchEvent(ev)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!mRedispatchTouchEvent) {
                    mScrollVertically = false
                    mRecyclerView?.postIdleTask {
                        updateStickyHeader(this, RecyclerView.SCROLL_STATE_DRAGGING)
                        updateStickyHeader(this, RecyclerView.SCROLL_STATE_IDLE)
                    }
                }
                mTouchHeaders = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return super.canScrollVertically(direction) || mRecyclerView?.canScrollVertically(direction) ?: false
    }

    //region private
    private fun findParent(): View? {
        var recyclerViewParent = mRecyclerView?.parent ?: return null
        if (recyclerViewParent !is ViewGroup) return null
        val set: HashSet<View> = hashSetOf()
        while (recyclerViewParent is ViewGroup) {
            if (contains(recyclerViewParent, this, set)) {
                return recyclerViewParent
            }
            set.add(recyclerViewParent)
            recyclerViewParent = recyclerViewParent.parent
        }
        return null
    }

    private fun contains(viewGroup: ViewGroup, target: View, set: HashSet<View>): Boolean {
        for (index in 0 until viewGroup.childCount) {
            val view = viewGroup[index]
            if (target == view) {
                return true
            } else if (view is ViewGroup) {
                if (set.contains(view)) {
                    continue
                }
                if (contains(view, target, set)) {
                    return true
                }
                set.add(view)
            }
        }
        return false
    }

    private fun isTouchHeaders(ev: MotionEvent): Boolean {
        for (index in 0 until childCount) {
            if (pointInView(getChildAt(index), ev.x, ev.y, 0F)) {
                return true
            }
        }
        return false
    }

    private fun pointInView(view: View, localX: Float, localY: Float, slop: Float): Boolean {
        return localX >= -slop && localY >= -slop && localX < view.right - view.left + slop &&
                localY < view.bottom - view.top + slop
    }

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
        val rect = Rect()


        return StickyHeader(viewHolder, position, rect)
    }

    private fun attachStickyHeader(position: Int, header: StickyHeader) {
        if (mAttachedHeaders.indexOfKey(position) < 0) {
            mAttachedHeaders.put(position, header)
            addView(header.viewHolder.itemView)
        }
    }

    private fun detachStickyHeader(position: Int) {
        val header = mAttachedHeaders.get(position)
        if (mAttachedHeaders.indexOfKey(position) >= 0) {
            mAttachedHeaders.remove(position)
        }
        if (header != null) {
            removeView(header.viewHolder.itemView)
        }
    }

    private fun isHeaderAttached(position: Int): Boolean {
        return mAttachedHeaders.indexOfKey(position) >= 0
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

    private fun RecyclerView.postIdleTask(task: RecyclerView.() -> Unit) {
        Looper.myQueue().addIdleHandler {
            task()
            false
        }
    }

    private val mTempRect = Rect()
    private val mDecorInsets = Rect()

    private fun getItemDecorInsetsForChild(child: View): Rect {
        val insets = mDecorInsets
        insets.set(0, 0, 0, 0)
        val decorCount = mRecyclerView?.itemDecorationCount ?: 0
        for (i in 0 until decorCount) {
            mTempRect.set(0, 0, 0, 0)
            mRecyclerView?.getItemDecorationAt(i)
                ?.getItemOffsets(mTempRect, child, mRecyclerView!!, mRecyclerViewState!!)
            insets.left += mTempRect.left
            insets.top += mTempRect.top
            insets.right += mTempRect.right
            insets.bottom += mTempRect.bottom
        }
        return insets
    }

    private fun RecyclerView.getRecyclerViewState(): RecyclerView.State {
        //RecyclerView::class.java  mState
        var state: RecyclerView.State? = null
        try {
            val field = RecyclerView::class.java.getDeclaredField("mState")
            field.isAccessible = true
            state = field.get(this) as RecyclerView.State
        } catch (ignore: Throwable) {
        }
        if (state != null) return state
        try {
            val fields = RecyclerView::class.java.declaredFields
            fields.forEach {
                if (it.declaringClass == RecyclerView.State::class.java) {
                    it.isAccessible = true
                    state = it.get(this) as RecyclerView.State
                    return@forEach
                }
            }
        } catch (ignore: Throwable) {
        }
        return state ?: RecyclerView.State()
    }

    private inline fun <T> SparseArray<T>.forEach(action: (key: Int, value: T) -> Unit) {
        for (index in 0 until size()) {
            action(keyAt(index), valueAt(index))
        }
    }
    //endregion

    private inner class RecyclerViewDataObserver : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            super.onChanged()
            mRecyclerView?.postIdleTask {
                updateStickyHeader(this)
                if (mAttachedHeaders.size() > 0) {
                    mAttachedHeaders.forEach { key, value ->
                        bindHeaderViewHolder(value, key, true)
                    }
                }
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            mRecyclerView?.postIdleTask { updateStickyHeader(this) }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            mRecyclerView?.postIdleTask { updateStickyHeader(this) }
        }

        override fun onStateRestorationPolicyChanged() {
            super.onStateRestorationPolicyChanged()
            mRecyclerView?.postIdleTask { updateStickyHeader(this) }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            mRecyclerView?.postIdleTask { updateStickyHeader(this) }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            mRecyclerView?.postIdleTask {
                updateStickyHeader(this)
                if (mAttachedHeaders.size() > 0) {
                    mAttachedHeaders.forEach { key, value ->
                        if (key >= positionStart && key < (positionStart + itemCount)) {
                            bindHeaderViewHolder(value, key, true)
                        }
                    }
                }
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            mRecyclerView?.postIdleTask {
                updateStickyHeader(this)
                if (mAttachedHeaders.size() > 0) {
                    mAttachedHeaders.forEach { key, value ->
                        if (key >= positionStart && key < (positionStart + itemCount)) {
                            bindHeaderViewHolder(value, key, true)
                        }
                    }
                }
            }
        }
    }


    /*public class LayoutParams() :
        FrameLayout.LayoutParams(int width, int height) {

        val viewHolder: RecyclerView.ViewHolder
        val decorInsets: Rect = Rect()
        var bound: Boolean = false
        var insetsDirty = true

    }*/


    private class StickyHeader(
        val viewHolder: RecyclerView.ViewHolder,
        val position: Int,
        val decorInsets: Rect
    ) {
        var bound: Boolean = false
        var insetsDirty = true
    }

    //public interface
}