package com.hyh.widget.sticky

import android.content.Context
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.hyh.demo.R
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/11/30
 */
class StickyItemsLayout : ViewGroup {

    private var maxStickyHeaders = 3
    private var maxStickyFooters = 1

    private val recyclerViewDataObserver = RecyclerViewDataObserver()
    private var cacheAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    private var visibleItemFinder: VisibleItemFinder = DefaultVisibleItemFinder()

    private lateinit var recyclerView: RecyclerView
    private lateinit var stickyItemsAdapter: IStickyItemsAdapter<RecyclerView.ViewHolder>

    private var stickyItemDecoration: StickyItemDecoration? = null

    private var headersOffsetY: Float = 0.0F
    private var headersHeight: Float = 0.0F

    private var firstAttachedHeader: StickyHeader? = null
    private var lastAttachedHeader: StickyHeader? = null
    private val attachedHeaderMap: MutableMap<Int, StickyHeader> = mutableMapOf()

    private var initialTouchX: Int = 0
    private var initialTouchY: Int = 0
    private var lastTouchX: Int = 0
    private var lastTouchY: Int = 0
    private val touchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    private var scrollVertically = false
    private var isTouchHeaders = false
    private var redispatchTouchEvent = false

    private val onScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            updateStickyHeader()
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            updateStickyHeader()
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Suppress("UNCHECKED_CAST")
    fun setup(recyclerView: RecyclerView, adapter: IStickyItemsAdapter<*>) {
        this.recyclerView = recyclerView
        this.stickyItemsAdapter = adapter as IStickyItemsAdapter<RecyclerView.ViewHolder>
        recyclerView.addOnScrollListener(onScrollListener)
        registerAdapterDataObserver(recyclerView)
    }

    fun setStickyItemDecoration(decoration: StickyItemDecoration) {
        this.stickyItemDecoration = decoration
        updateStickyHeader(recyclerView.scrollState, true)
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val attachedHeader = this.firstAttachedHeader
        if (attachedHeader == null) {
            this.headersHeight = 0.0F
        } else {
            var headersHeight = 0.0F
            attachedHeader.iterator().forEach {
                measureChildWithMargins(
                    it.viewHolder.itemView,
                    widthMeasureSpec,
                    heightMeasureSpec
                )
                headersHeight += it.heightWithDecor
            }
            this.headersHeight = headersHeight
        }
        updateHeadersOffsetY()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val layoutParams = child.layoutParams as LayoutParams
            val childLeft: Int = paddingLeft + layoutParams.leftMargin + layoutParams.insets.left
            val childTop: Int = paddingTop + layoutParams.topMargin + layoutParams.insets.top
            val childRight: Int = childLeft + child.measuredWidth
            val childBottom: Int = childTop + child.measuredHeight
            child.layout(childLeft, childTop, childRight, childBottom)
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    private fun updateHeadersOffsetY() {
        val nextHeaderBounds = getNextHeaderBounds(lastAttachedHeader?.position ?: -1)
        this.headersOffsetY = if (nextHeaderBounds != null
            && nextHeaderBounds.top < headersHeight
        ) {
            (nextHeaderBounds.top - headersHeight)
        } else {
            0.0F
        }
        firstAttachedHeader?.iterator()?.forEach {
            it.updateOffsetY()
        }
    }

    private fun measureChildWithMargins(child: View, widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val lp = child.layoutParams as LayoutParams
        val insets: Rect = getItemDecorInsetsForChild(child)
        val childWidthMeasureSpec = getChildMeasureSpec(
            widthMeasureSpec,
            paddingLeft + paddingRight
                    + lp.leftMargin + lp.rightMargin
                    + insets.left + insets.right,
            lp.width
        )

        val childHeightMeasureSpec = getChildMeasureSpec(
            heightMeasureSpec,
            paddingTop + paddingBottom
                    + lp.topMargin + lp.bottomMargin
                    + insets.top + insets.bottom,
            lp.height
        )

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)

    }

    private fun getItemDecorInsetsForChild(child: View): Rect {
        val lp =
            child.layoutParams as LayoutParams
        if (!lp.insetsDirty) {
            return lp.insets
        }
        val insets = lp.insets
        insets.set(0, 0, 0, 0)
        stickyItemDecoration?.getItemOffsets(insets, lp.adapterPosition, this)
        lp.insetsDirty = false
        return insets
    }

    private fun updateStickyHeader(
        scrollState: Int = recyclerView.scrollState,
        dataChanged: Boolean = false
    ) {
        registerAdapterDataObserver(recyclerView)

        val adapter = recyclerView.adapter
        val positions = findCurrentHeadersPosition(recyclerView)
        if (adapter == null || positions.isEmpty()) {
            var attachedHeader = this.firstAttachedHeader
            while (attachedHeader != null) {
                attachedHeader.bindItemViewHolder()
                attachedHeader.detach()
                attachedHeader = attachedHeader.next
            }
            this.firstAttachedHeader = null
            this.lastAttachedHeader = null
            this.headersOffsetY = 0.0F
            this.headersHeight = 0.0F
            this.attachedHeaderMap.clear()
            return
        }

        if (this.firstAttachedHeader == null || dataChanged) {

            var firstAttachedHeader: StickyHeader? = null
            var lastAttachedHeader: StickyHeader? = null

            var oldAttachedHeader: StickyHeader? = this.firstAttachedHeader

            this.attachedHeaderMap.clear()
            positions.forEach {
                val itemViewType = adapter.getItemViewType(it)
                val stickyHeader: StickyHeader
                val tempOldAttachedHeader = oldAttachedHeader
                if (tempOldAttachedHeader != null) {
                    if (tempOldAttachedHeader.itemViewType == itemViewType) {
                        stickyHeader = createStickyHeader(adapter, it, tempOldAttachedHeader)
                    } else {
                        stickyHeader = createStickyHeader(adapter, it)
                        stickyHeader.attach()
                        tempOldAttachedHeader.detach()
                    }
                } else {
                    stickyHeader = createStickyHeader(adapter, it)
                    stickyHeader.attach()
                }
                oldAttachedHeader = tempOldAttachedHeader?.next

                if (firstAttachedHeader == null) {
                    firstAttachedHeader = stickyHeader
                    lastAttachedHeader = stickyHeader
                } else {
                    lastAttachedHeader?.next = stickyHeader
                    stickyHeader.prev = lastAttachedHeader
                    lastAttachedHeader = stickyHeader
                }
                this.attachedHeaderMap[it] = stickyHeader
            }

            this.firstAttachedHeader = firstAttachedHeader
            this.lastAttachedHeader = lastAttachedHeader

            this.firstAttachedHeader?.iterator()?.forEach {
                it.bindHeaderViewHolder(true)
            }
        } else {
            var firstAttachedHeader: StickyHeader? = null
            var lastAttachedHeader: StickyHeader? = null

            positions.forEach {
                val stickyHeader = attachedHeaderMap.remove(it)
                    ?: createStickyHeader(adapter, it).apply {
                        attach()
                        bindHeaderViewHolder(true)
                    }
                if (firstAttachedHeader == null) {
                    firstAttachedHeader = stickyHeader
                    lastAttachedHeader = stickyHeader
                    removePrev(stickyHeader)
                } else {
                    lastAttachedHeader?.next = stickyHeader
                    stickyHeader.prev = lastAttachedHeader
                    lastAttachedHeader = stickyHeader
                }
            }

            removeNext(lastAttachedHeader)

            val iterator = attachedHeaderMap.entries.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                next.value.detach()
                iterator.remove()
            }

            this.firstAttachedHeader = firstAttachedHeader
            this.lastAttachedHeader = lastAttachedHeader

            this.firstAttachedHeader?.iterator()?.forEach {
                attachedHeaderMap[it.position] = it
                if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    it.bindHeaderViewHolder()
                } else {
                    it.bindItemViewHolder()
                }
            }
        }

        updateHeadersOffsetY()
    }

    private fun removePrev(stickyHeader: StickyHeader?) {
        val prev = stickyHeader?.prev ?: return
        stickyHeader.prev = null
        prev.detach()
        removePrev(prev)
    }

    private fun removeNext(stickyHeader: StickyHeader?) {
        val next = stickyHeader?.next ?: return
        stickyHeader.next = null
        next.detach()
        removeNext(next)
    }

    private fun getNextHeaderBounds(lastAttachedHeaderPosition: Int): Rect? {
        if (attachedHeaderMap.size < maxStickyHeaders) return null
        if (lastAttachedHeaderPosition < 0) return null
        val nextHeaderPosition = findNextHeaderPosition(recyclerView, lastAttachedHeaderPosition)
        if (nextHeaderPosition < 0) return null
        val nextHeaderViewHolder =
            recyclerView.findViewHolderForAdapterPosition(nextHeaderPosition) ?: return null

        val stickyItemDecoration = this.stickyItemDecoration
        return if (stickyItemDecoration == null) {
            Rect(
                nextHeaderViewHolder.itemView.left,
                nextHeaderViewHolder.itemView.top,
                nextHeaderViewHolder.itemView.right,
                nextHeaderViewHolder.itemView.bottom
            )
        } else {
            val outRect = Rect()
            stickyItemDecoration.getItemOffsets(outRect, nextHeaderPosition, this)
            Rect(
                nextHeaderViewHolder.itemView.left - outRect.left,
                nextHeaderViewHolder.itemView.top - outRect.top,
                nextHeaderViewHolder.itemView.right + outRect.right,
                nextHeaderViewHolder.itemView.bottom + outRect.bottom
            )
        }
    }

    private fun findItemViewBounds(position: Int): Rect? {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return null
        val stickyItemDecoration = this.stickyItemDecoration
        return if (stickyItemDecoration == null) {
            Rect(
                viewHolder.itemView.left,
                viewHolder.itemView.top,
                viewHolder.itemView.right,
                viewHolder.itemView.bottom
            )
        } else {
            val outRect = Rect()
            stickyItemDecoration.getItemOffsets(outRect, position, this)
            Rect(
                viewHolder.itemView.left - outRect.left,
                viewHolder.itemView.top - outRect.top,
                viewHolder.itemView.right + outRect.right,
                viewHolder.itemView.bottom + outRect.bottom
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (redispatchTouchEvent) {
                    redispatchTouchEvent = false
                    scrollVertically = false
                    return false
                }
                initialTouchX = ev.x.roundToInt()
                initialTouchY = ev.y.roundToInt()
                lastTouchX = initialTouchX
                lastTouchY = initialTouchY
                isTouchHeaders = firstAttachedHeader != null && isTouchHeaders(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isTouchHeaders) {
                    return super.dispatchTouchEvent(ev)
                }
                if (scrollVertically) {
                    return false
                }
                val curX = ev.x.roundToInt()
                val curY = ev.y.roundToInt()

                val tx = abs(curX - initialTouchX)
                val ty = abs(curY - initialTouchY)

                if (tx > touchSlop || ty > touchSlop) {
                    if (ty > tx) {
                        return findParent()?.let {
                            scrollVertically = true
                            val newEvent = MotionEvent.obtain(ev)
                            newEvent.action = MotionEvent.ACTION_DOWN
                            redispatchTouchEvent = true
                            it.dispatchTouchEvent(newEvent)
                            false
                        } ?: super.dispatchTouchEvent(ev)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!redispatchTouchEvent) {
                    scrollVertically = false
                    recyclerView.postIdleTask {
                        updateStickyHeader(RecyclerView.SCROLL_STATE_DRAGGING)
                        updateStickyHeader(RecyclerView.SCROLL_STATE_IDLE)
                    }
                }
                isTouchHeaders = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return super.canScrollVertically(direction) || recyclerView.canScrollVertically(direction)
    }

    private fun findParent(): View? {
        var recyclerViewParent = recyclerView.parent ?: return null
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

    /*private fun isTouchHeaders(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        return x >= left && y >= top && x < right &&
                y < top + headersHeight
    }*/


    private fun isTouchHeaders(ev: MotionEvent): Boolean {
        for (index in 0 until childCount) {
            if (isTransformedTouchPointInView(ev.x, ev.y, getChildAt(index))) {
                return true
            }
        }
        return false
    }

    private fun findTouchHeader(ev: MotionEvent): StickyHeader? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (isTransformedTouchPointInView(ev.x, ev.y, getChildAt(index))) {
                return child.getTag(R.id.sticky_item_tag) as? StickyHeader
            }
        }
        return null
    }

    private fun isTransformedTouchPointInView(
        x: Float, y: Float, child: View
    ): Boolean {
        val point = floatArrayOf(x, y)
        point[0] += (scrollX - child.left).toFloat()
        point[1] += (scrollY - child.top).toFloat()

        point[0] -= child.translationX
        point[1] -= child.translationY

        return pointInView(child, point[0], point[1])
    }

    private fun pointInView(view: View, localX: Float, localY: Float, slop: Float = 0.0F): Boolean {
        return localX >= -slop && localY >= -slop && localX < view.right - view.left + slop &&
                localY < view.bottom - view.top + slop
    }

    private fun createStickyHeader(
        adapter: RecyclerView.Adapter<*>,
        position: Int
    ): StickyHeader {
        val itemViewType = adapter.getItemViewType(position)
        val headerViewHolder: RecyclerView.ViewHolder =
            recyclerView.recycledViewPool.getRecycledView(itemViewType)
                ?: adapter.onCreateViewHolder(recyclerView, itemViewType)
        val decorInsets = Rect()
        stickyItemDecoration?.getItemOffsets(decorInsets, position, this)
        return StickyHeader(
            position,
            itemViewType,
            headerViewHolder,
            headerViewHolder.itemView.layoutParams
        ).apply {
            headerViewHolder.itemView.setTag(R.id.sticky_item_tag, this)
        }
    }

    private fun createStickyHeader(
        adapter: RecyclerView.Adapter<*>,
        position: Int,
        recycled: StickyHeader,
    ): StickyHeader {
        val itemViewType = adapter.getItemViewType(position)
        val decorInsets = Rect()
        stickyItemDecoration?.getItemOffsets(decorInsets, position, this)
        return StickyHeader(
            position,
            itemViewType,
            recycled.viewHolder,
            recycled.originalLayoutParams
        ).apply {
            viewHolder.itemView.setTag(R.id.sticky_item_tag, this)
        }
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return stickyItemsAdapter.isStickyHeader(position)
    }

    private fun isFooterPosition(position: Int): Boolean {
        return stickyItemsAdapter.isStickyFooter(position)
    }

    private fun findCurrentHeadersPosition(recyclerView: RecyclerView): Collection<Int> {
        if (recyclerView.computeVerticalScrollRange() <= 0) {
            return emptyList()
        }
        val firstCompletelyVisibleItemPosition =
            visibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
        if (firstCompletelyVisibleItemPosition == 0) return emptyList()
        val lastVisibleItemPosition =
            visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
        if (lastVisibleItemPosition <= firstCompletelyVisibleItemPosition) {
            return emptyList()
        }
        if (lastVisibleItemPosition - firstCompletelyVisibleItemPosition <= maxStickyHeaders) {
            return (firstCompletelyVisibleItemPosition..lastVisibleItemPosition).toList()
        }

        val positions = TreeSet<Int>()


        for (position in (firstCompletelyVisibleItemPosition - 1) downTo 0) {
            if (isHeaderPosition(position)) {
                positions.add(position)
            }
            if (positions.size >= maxStickyHeaders) {
                break
            }
        }

        for (position in firstCompletelyVisibleItemPosition..lastVisibleItemPosition) {
            if (isHeaderPosition(position)) {
                val bounds = findItemViewBounds(position) ?: return positions
                if (positions.size >= maxStickyHeaders) {
                    if (bounds.bottom < headersHeight) {
                        positions.remove(positions.first())
                        positions.add(position)
                    }
                } else {
                    val stickyHeader = attachedHeaderMap[position]
                    if (stickyHeader != null) {
                        if (bounds.top < headersHeight - stickyHeader.heightWithDecor) {
                            positions.add(position)
                            if (positions.size >= maxStickyHeaders) {
                                break
                            }
                        }
                    } else {
                        if (bounds.top < headersHeight) {
                            positions.add(position)
                            if (positions.size >= maxStickyHeaders) {
                                break
                            }
                        }
                    }
                }
            }
        }

        return positions
    }

    private fun findCurrentFootersPosition(recyclerView: RecyclerView): List<Int> {
        val lastCompletelyVisibleItemPosition =
            visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
        val itemCount = getItemCount()
        if (lastCompletelyVisibleItemPosition + 1 >= itemCount) return emptyList()
        val positions = mutableListOf<Int>()
        for (position in (lastCompletelyVisibleItemPosition + 1) until itemCount) {
            if (isFooterPosition(position)) {
                positions.add(position)
            }
            if (positions.size >= maxStickyFooters) {
                break
            }
        }
        return positions
    }

    private fun isVisibleItem(position: Int): Boolean {
        val firstVisibleItemPosition =
            visibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        val lastVisibleItemPosition =
            visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
        if (firstVisibleItemPosition < 0) return false
        if (firstVisibleItemPosition > lastVisibleItemPosition) return false
        return position in firstVisibleItemPosition..lastVisibleItemPosition
    }

    private fun findLastHeaderPosition(recyclerView: RecyclerView): Int {
        val firstVisibleItemPosition =
            visibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        if (firstVisibleItemPosition <= 0) {
            return RecyclerView.NO_POSITION
        }
        return RecyclerView.NO_POSITION
    }

    private fun findNextHeaderPosition(recyclerView: RecyclerView, lastHeaderPosition: Int): Int {
        val firstVisibleItemPosition =
            visibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
        val lastVisibleItemPosition =
            visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
        return if (firstVisibleItemPosition == lastHeaderPosition) {
            findFirstHeaderPosition(firstVisibleItemPosition + 1, lastVisibleItemPosition)
        } else if (firstVisibleItemPosition > lastHeaderPosition) {
            findFirstHeaderPosition(firstVisibleItemPosition, lastVisibleItemPosition)
        } else {
            findFirstHeaderPosition(lastHeaderPosition + 1, lastVisibleItemPosition)
        }
    }

    private fun findFirstHeaderPosition(startPosition: Int, endPosition: Int): Int {
        if (startPosition > endPosition) return RecyclerView.NO_POSITION
        return stickyItemsAdapter.let {
            for (position in startPosition..endPosition) {
                if (it.isStickyHeader(position)) return@let position
            }
            RecyclerView.NO_POSITION
        }
    }

    private fun registerAdapterDataObserver(recyclerView: RecyclerView) {
        val adapter = recyclerView.adapter
        if (adapter == this.cacheAdapter) return
        (recyclerView.parent as ViewGroup).isMotionEventSplittingEnabled = false
        if (this.cacheAdapter != null) {
            this.cacheAdapter?.unregisterAdapterDataObserver(recyclerViewDataObserver)
        }
        this.cacheAdapter = adapter
        recyclerViewDataObserver.onChanged()
        adapter?.registerAdapterDataObserver(recyclerViewDataObserver)
    }

    private fun RecyclerView.postIdleTask(task: RecyclerView.() -> Unit) {
        Looper.myQueue().addIdleHandler {
            task()
            false
        }
    }

    private fun getItemCount(): Int {
        return recyclerView.adapter?.itemCount ?: 0
    }

    //endregion


    class LayoutParams : MarginLayoutParams {

        var insetsDirty = true

        val insets: Rect = Rect()

        var adapterPosition = -1

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: MarginLayoutParams) : super(source)

        constructor(source: ViewGroup.LayoutParams) : super(source)
    }

    private inner class RecyclerViewDataObserver : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            super.onChanged()
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }

        override fun onStateRestorationPolicyChanged() {
            super.onStateRestorationPolicyChanged()
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            recyclerView.postIdleTask { updateStickyHeader(dataChanged = true) }
        }
    }


    private inner class StickyHeader(
        val position: Int,
        val itemViewType: Int,
        val viewHolder: RecyclerView.ViewHolder,
        val originalLayoutParams: ViewGroup.LayoutParams?,
    ) {

        val index: Int
            get() {
                return prev?.let {
                    it.index + 1
                } ?: 0
            }

        var prev: StickyHeader? = null

        var next: StickyHeader? = null

        private val layoutParams: LayoutParams = if (originalLayoutParams == null) {
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                viewHolder.itemView.layoutParams = this
            }
        } else {
            LayoutParams(originalLayoutParams).apply {
                viewHolder.itemView.layoutParams = this
            }
        }

        var bound: Boolean = false
        val heightWithDecor: Int
            get() = viewHolder.itemView.measuredHeight + layoutParams.insets.top + layoutParams.insets.bottom

        val offsetY: Float
            get() = viewHolder.itemView.translationY


        fun attach() {
            addView(viewHolder.itemView, layoutParams)
        }

        fun bindHeaderViewHolder(force: Boolean = false) {
            if (!force && bound) return
            stickyItemsAdapter.onBindStickyViewHolder(viewHolder, position)
            bound = true
        }

        fun bindItemViewHolder() {
            if (!isVisibleItem(position)) return
            val adapter = recyclerView.adapter ?: return
            if (adapter.getItemViewType(position) != itemViewType) return
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return
            adapter.bindViewHolder(viewHolder, position)
            bound = false
        }

        fun updateOffsetY() {
            if (prev != null) {
                val offsetY = (prev?.offsetY ?: 0.0F) + (prev?.heightWithDecor ?: 0)
                viewHolder.itemView.translationY = offsetY
            } else {
                viewHolder.itemView.translationY = headersOffsetY
            }
        }

        fun detach() {
            viewHolder.itemView.removeFromParent()
        }

        private fun View?.removeFromParent() {
            val parent = this?.parent ?: return
            if (parent !is ViewGroup) return
            parent.removeView(this)
        }

        fun iterator(): Iterator<StickyHeader> {
            var cursor: StickyHeader? = this
            return object : Iterator<StickyHeader> {

                override fun hasNext(): Boolean {
                    return cursor != null
                }

                override fun next(): StickyHeader {
                    val result = cursor!!
                    cursor = result.next
                    return result
                }
            }
        }

        fun listIterator(): ListIterator<StickyHeader> {
            var nextCursor: StickyHeader? = this
            var prevCursor: StickyHeader? = prev
            return object : ListIterator<StickyHeader> {

                override fun hasNext(): Boolean {
                    return nextCursor != null
                }

                override fun hasPrevious(): Boolean {
                    return prevCursor != null
                }

                override fun next(): StickyHeader {
                    val result = nextCursor!!
                    prevCursor = result
                    nextCursor = result.next
                    return result
                }

                override fun nextIndex(): Int {
                    return nextCursor!!.index
                }

                override fun previous(): StickyHeader {
                    val result = prevCursor!!
                    nextCursor = result
                    prevCursor = result.prev
                    return result
                }

                override fun previousIndex(): Int {
                    return prevCursor!!.index
                }
            }
        }
    }
}