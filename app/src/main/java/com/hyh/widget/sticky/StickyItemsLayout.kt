package com.hyh.widget.sticky

import android.content.Context
import android.graphics.Canvas
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
 * 支持 [RecyclerView] 固定顶部与固定底部的布局
 *
 * @author eriche
 * @data 2020/11/30
 */
class StickyItemsLayout : ViewGroup {

    private var maxStickyHeaders = 3
    private var maxStickyFooters = 3

    private val rectPool = RectPool()

    private val recyclerViewDataObserver = RecyclerViewDataObserver()
    private var cacheAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    private var visibleItemFinder: VisibleItemFinder = DefaultVisibleItemFinder()

    private lateinit var recyclerView: RecyclerView
    private lateinit var stickyItemsAdapter: IStickyItemsAdapter<RecyclerView.ViewHolder>

    private var stickyItemDecoration: StickyItemDecoration? = null

    private val stickyHeaders = StickyHeaders()
    private val stickyFooters = StickyFooters()

    private var initialTouchX: Int = 0
    private var initialTouchY: Int = 0
    private var lastTouchX: Int = 0
    private var lastTouchY: Int = 0
    private val touchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    private var scrollVertically = false
    private var touchedStickyItems: StickyItems? = null
    private var redispatchTouchEvent = false

    private val onScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            stickyHeaders.updateStickyItem()
            stickyFooters.updateStickyItem()
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            stickyHeaders.updateStickyItem()
            stickyFooters.updateStickyItem()
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setMaxStickyItems(maxStickyHeaders: Int, maxStickyFooters: Int) {
        this.maxStickyHeaders = maxStickyHeaders
        this.maxStickyFooters = maxStickyFooters
        stickyHeaders.updateStickyItem(recyclerView.scrollState, true)
        stickyFooters.updateStickyItem(recyclerView.scrollState, true)
    }

    @Suppress("UNCHECKED_CAST")
    fun setup(recyclerView: RecyclerView, adapter: IStickyItemsAdapter<*>) {
        this.recyclerView = recyclerView
        this.stickyItemsAdapter = adapter as IStickyItemsAdapter<RecyclerView.ViewHolder>
        recyclerView.addOnScrollListener(onScrollListener)
        registerAdapterDataObserver(recyclerView)
    }

    fun setStickyItemDecoration(decoration: StickyItemDecoration) {
        this.stickyItemDecoration = decoration
        stickyHeaders.updateStickyItem(recyclerView.scrollState, true)
        stickyFooters.updateStickyItem(recyclerView.scrollState, true)
    }

    fun getDecoratedBoundsWithMargins(view: View, outBounds: Rect) {
        val lp = view.layoutParams as? LayoutParams ?: return
        val insets = lp.insets
        outBounds.set(
            view.left - insets.left - lp.leftMargin,
            view.top - insets.top - lp.topMargin,
            view.right + insets.right + lp.rightMargin,
            view.bottom + insets.bottom + lp.bottomMargin
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        stickyHeaders.onMeasure(widthMeasureSpec, heightMeasureSpec)
        stickyFooters.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        stickyHeaders.onLayout()
        stickyFooters.onLayout()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        stickyItemDecoration?.onDrawOver(canvas, this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        stickyItemDecoration?.onDraw(canvas, this)
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        fun contains(viewGroup: ViewGroup, target: View, set: HashSet<View>): Boolean {
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

        fun findParent(): View? {
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
                touchedStickyItems = findStickyItems(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchedStickyItems == null) {
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
                    touchedStickyItems?.let {
                        recyclerView.postIdleTask {
                            it.updateStickyItem(RecyclerView.SCROLL_STATE_DRAGGING)
                            it.updateStickyItem(RecyclerView.SCROLL_STATE_IDLE)
                        }
                    }
                }
                touchedStickyItems = null
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return super.canScrollVertically(direction) || recyclerView.canScrollVertically(direction)
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


    private fun findStickyItems(ev: MotionEvent): StickyItems? {
        if (stickyHeaders.isPointIn(ev)) {
            return stickyHeaders
        }
        if (stickyFooters.isPointIn(ev)) {
            return stickyFooters
        }
        return null
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return stickyItemsAdapter.isStickyHeader(position)
    }

    private fun isFooterPosition(position: Int): Boolean {
        return stickyItemsAdapter.isStickyFooter(position)
    }

    private fun findItemViewBounds(position: Int): Rect? {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return null
        val stickyItemDecoration = this.stickyItemDecoration
        return if (stickyItemDecoration == null) {
            rectPool.obtain(
                viewHolder.itemView.left,
                viewHolder.itemView.top,
                viewHolder.itemView.right,
                viewHolder.itemView.bottom
            )
        } else {
            val outRect = rectPool.obtain()
            stickyItemDecoration.getItemOffsets(outRect, position, this)
            outRect.set(
                viewHolder.itemView.left - outRect.left,
                viewHolder.itemView.top - outRect.top,
                viewHolder.itemView.right + outRect.right,
                viewHolder.itemView.bottom + outRect.bottom
            )
            outRect
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
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }

        override fun onStateRestorationPolicyChanged() {
            super.onStateRestorationPolicyChanged()
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            recyclerView.postIdleTask {
                stickyHeaders.updateStickyItem(dataChanged = true)
                stickyFooters.updateStickyItem(dataChanged = true)
            }
        }
    }


    private abstract inner class StickyItems {

        protected var firstAttachedItem: StickyItem? = null
        protected var lastAttachedItem: StickyItem? = null
        protected val attachedItemMap: MutableMap<Int, StickyItem> = mutableMapOf()
        protected var itemsOffsetY: Float = 0.0F
        protected var itemsHeight: Float = 0.0F


        fun isPointIn(ev: MotionEvent): Boolean {
            val firstAttachedItem = firstAttachedItem ?: return false
            firstAttachedItem.iterator().forEach {
                if (isTransformedTouchPointInView(ev.x, ev.y, it.itemView)) {
                    return true
                }
            }
            return false
        }

        fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val attachedHeader = this.firstAttachedItem
            if (attachedHeader == null) {
                this.itemsHeight = 0.0F
            } else {
                var headersHeight = 0.0F
                attachedHeader.iterator().forEach {
                    measureChildWithMargins(
                        it.itemView,
                        widthMeasureSpec,
                        heightMeasureSpec
                    )
                    headersHeight += it.heightWithDecor
                }
                this.itemsHeight = headersHeight
            }
            updateItemsOffsetY()
        }


        abstract fun onLayout()

        open fun updateStickyItem(
            scrollState: Int = recyclerView.scrollState,
            dataChanged: Boolean = false
        ) {
            val adapter = recyclerView.adapter
            val positions = findCurrentItemsPosition(recyclerView)
            if (adapter == null || positions.isEmpty()) {
                var attachedItem = this.firstAttachedItem
                while (attachedItem != null) {
                    attachedItem.bindItemViewHolder()
                    attachedItem.detach()
                    attachedItem = attachedItem.next
                }
                this.firstAttachedItem = null
                this.lastAttachedItem = null
                this.itemsOffsetY = 0.0F
                this.itemsHeight = 0.0F
                this.attachedItemMap.clear()
                return
            }

            if (this.firstAttachedItem == null || dataChanged) {

                var firstAttachedItem: StickyItem? = null
                var lastAttachedItem: StickyItem? = null

                var oldAttachedItem: StickyItem? = this.firstAttachedItem

                this.attachedItemMap.clear()
                positions.forEach {
                    val itemViewType = adapter.getItemViewType(it)
                    val stickyItem: StickyItem
                    val tempOldAttachedItem = oldAttachedItem
                    if (tempOldAttachedItem != null) {
                        if (tempOldAttachedItem.itemViewType == itemViewType) {
                            stickyItem = createStickyItem(adapter, it, tempOldAttachedItem)
                        } else {
                            stickyItem = createStickyItem(adapter, it)
                            stickyItem.attach()
                            tempOldAttachedItem.detach()
                        }
                    } else {
                        stickyItem = createStickyItem(adapter, it)
                        stickyItem.attach()
                    }
                    oldAttachedItem = tempOldAttachedItem?.next

                    if (firstAttachedItem == null) {
                        firstAttachedItem = stickyItem
                        lastAttachedItem = stickyItem
                    } else {
                        lastAttachedItem?.next = stickyItem
                        stickyItem.prev = lastAttachedItem
                        lastAttachedItem = stickyItem
                    }
                    this.attachedItemMap[it] = stickyItem
                }

                this.firstAttachedItem = firstAttachedItem
                this.lastAttachedItem = lastAttachedItem

                this.firstAttachedItem?.iterator()?.forEach {
                    it.bindStickyItemViewHolder(true)
                }
            } else {
                var firstAttachedItem: StickyItem? = null
                var lastAttachedItem: StickyItem? = null

                positions.forEach {
                    val stickyItem = attachedItemMap.remove(it)
                        ?: createStickyItem(adapter, it).apply {
                            attach()
                            bindStickyItemViewHolder(true)
                        }
                    if (firstAttachedItem == null) {
                        firstAttachedItem = stickyItem
                        lastAttachedItem = stickyItem
                        removePrev(stickyItem)
                    } else {
                        lastAttachedItem?.next = stickyItem
                        stickyItem.prev = lastAttachedItem
                        lastAttachedItem = stickyItem
                    }
                }

                removeNext(lastAttachedItem)

                val iterator = attachedItemMap.entries.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    next.value.detach()
                    iterator.remove()
                }

                this.firstAttachedItem = firstAttachedItem
                this.lastAttachedItem = lastAttachedItem

                this.firstAttachedItem?.iterator()?.forEach {
                    attachedItemMap[it.position] = it
                    if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        it.bindStickyItemViewHolder()
                    } else {
                        it.bindItemViewHolder()
                    }
                }
            }

            updateItemsOffsetY()
        }

        abstract fun findCurrentItemsPosition(recyclerView: RecyclerView): Collection<Int>

        abstract fun createStickyItem(
            adapter: RecyclerView.Adapter<*>,
            position: Int,
            recycled: StickyItem? = null
        ): StickyItem

        abstract fun updateItemsOffsetY()

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

        private fun removePrev(stickyItem: StickyItem?) {
            val prev = stickyItem?.prev ?: return
            stickyItem.prev = null
            prev.detach()
            removePrev(prev)
        }

        private fun removeNext(stickyItem: StickyItem?) {
            val next = stickyItem?.next ?: return
            stickyItem.next = null
            next.detach()
            removeNext(next)
        }
    }


    private inner class StickyHeaders : StickyItems() {

        override fun onLayout() {
            firstAttachedItem?.iterator()?.forEach {
                val child = it.itemView
                val layoutParams = child.layoutParams as LayoutParams
                val childLeft: Int = paddingLeft + layoutParams.leftMargin + layoutParams.insets.left
                val childTop: Int = paddingTop + layoutParams.topMargin + layoutParams.insets.top
                val childRight: Int = childLeft + child.measuredWidth
                val childBottom: Int = childTop + child.measuredHeight
                child.layout(childLeft, childTop, childRight, childBottom)
            }
        }

        override fun findCurrentItemsPosition(recyclerView: RecyclerView): Collection<Int> {
            if (recyclerView.computeVerticalScrollRange() <= 0) return emptyList()

            val firstCompletelyVisibleItemPosition =
                visibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
            if (firstCompletelyVisibleItemPosition == 0) return emptyList()

            val lastCompletelyVisibleItemPosition =
                visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
            if (lastCompletelyVisibleItemPosition <= firstCompletelyVisibleItemPosition) return emptyList()


            val positions = TreeSet<Int>()

            for (position in (firstCompletelyVisibleItemPosition - 1) downTo 0) {
                if (isHeaderPosition(position)) {
                    positions.add(position)
                }
                if (positions.size >= maxStickyHeaders) {
                    break
                }
            }

            for (position in firstCompletelyVisibleItemPosition..lastCompletelyVisibleItemPosition) {
                if (isHeaderPosition(position)) {
                    val bounds = findItemViewBounds(position) ?: return positions
                    val top = bounds.top
                    val bottom = bounds.bottom
                    rectPool.recycle(bounds)
                    if (positions.size >= maxStickyHeaders) {
                        if (bottom < itemsHeight) {
                            positions.remove(positions.first())
                            positions.add(position)
                        }

                    } else {
                        val stickyHeader = attachedItemMap[position]
                        if (stickyHeader != null) {
                            if (top < itemsHeight - stickyHeader.heightWithDecor) {
                                positions.add(position)
                                if (positions.size >= maxStickyHeaders) {
                                    break
                                }
                            }
                        } else {
                            if (top < itemsHeight) {
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

        override fun createStickyItem(adapter: RecyclerView.Adapter<*>, position: Int, recycled: StickyItem?): StickyItem {
            return if (recycled != null) {
                createStickyHeader(adapter, position, recycled)
            } else {
                createStickyHeader(adapter, position)
            }
        }

        override fun updateItemsOffsetY() {
            val nextHeaderBounds = getNextHeaderBounds(lastAttachedItem?.position ?: -1)
            this.itemsOffsetY = if (nextHeaderBounds != null
                && nextHeaderBounds.top < itemsHeight
            ) {
                (nextHeaderBounds.top - itemsHeight)
            } else {
                0.0F
            }
            rectPool.recycle(nextHeaderBounds)
            firstAttachedItem?.iterator()?.forEach {
                it.updateOffsetY(itemsOffsetY)
            }
        }


        private fun createStickyHeader(
            adapter: RecyclerView.Adapter<*>,
            position: Int
        ): StickyItem {
            val itemViewType = adapter.getItemViewType(position)
            val headerViewHolder: RecyclerView.ViewHolder =
                recyclerView.recycledViewPool.getRecycledView(itemViewType)
                    ?: adapter.onCreateViewHolder(recyclerView, itemViewType)
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
            recycled: StickyItem,
        ): StickyItem {
            val itemViewType = adapter.getItemViewType(position)
            return StickyHeader(
                position,
                itemViewType,
                recycled.viewHolder,
                recycled.originalLayoutParams
            ).apply {
                viewHolder.itemView.setTag(R.id.sticky_item_tag, this)

            }
        }

        private fun getNextHeaderBounds(lastAttachedHeaderPosition: Int): Rect? {
            if (attachedItemMap.size < maxStickyHeaders) return null
            if (lastAttachedHeaderPosition < 0) return null
            val nextHeaderPosition = findNextHeaderPosition(recyclerView, lastAttachedHeaderPosition)
            if (nextHeaderPosition < 0) return null
            val nextHeaderViewHolder =
                recyclerView.findViewHolderForAdapterPosition(nextHeaderPosition) ?: return null

            val stickyItemDecoration = this@StickyItemsLayout.stickyItemDecoration
            return if (stickyItemDecoration == null) {
                rectPool.obtain(
                    nextHeaderViewHolder.itemView.left,
                    nextHeaderViewHolder.itemView.top,
                    nextHeaderViewHolder.itemView.right,
                    nextHeaderViewHolder.itemView.bottom
                )
            } else {
                val outRect = rectPool.obtain()
                stickyItemDecoration.getItemOffsets(outRect, nextHeaderPosition, this@StickyItemsLayout)
                outRect.set(
                    nextHeaderViewHolder.itemView.left - outRect.left,
                    nextHeaderViewHolder.itemView.top - outRect.top,
                    nextHeaderViewHolder.itemView.right + outRect.right,
                    nextHeaderViewHolder.itemView.bottom + outRect.bottom
                )
                outRect
            }
        }

        private fun findNextHeaderPosition(recyclerView: RecyclerView, lastHeaderPosition: Int): Int {
            val firstVisibleItemPosition =
                visibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
            val lastCompletelyVisibleItemPosition =
                visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
            return if (firstVisibleItemPosition == lastHeaderPosition) {
                findFirstHeaderPosition(firstVisibleItemPosition + 1, lastCompletelyVisibleItemPosition)
            } else if (firstVisibleItemPosition > lastHeaderPosition) {
                findFirstHeaderPosition(firstVisibleItemPosition, lastCompletelyVisibleItemPosition)
            } else {
                findFirstHeaderPosition(lastHeaderPosition + 1, lastCompletelyVisibleItemPosition)
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
    }


    private inner class StickyFooters : StickyItems() {


        override fun onLayout() {
            firstAttachedItem?.iterator()?.forEach {
                val child = it.itemView
                val layoutParams = child.layoutParams as LayoutParams
                val childLeft: Int = paddingLeft + layoutParams.leftMargin + layoutParams.insets.left
                val childTop: Int = height -
                        paddingBottom -
                        layoutParams.bottomMargin -
                        layoutParams.insets.bottom -
                        child.measuredHeight
                val childRight: Int = childLeft + child.measuredWidth
                val childBottom: Int = childTop + child.measuredHeight
                child.layout(childLeft, childTop, childRight, childBottom)
            }
        }

        override fun findCurrentItemsPosition(recyclerView: RecyclerView): Collection<Int> {
            if (recyclerView.computeVerticalScrollRange() <= 0) return emptyList()

            val lastCompletelyVisibleItemPosition =
                visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)

            if (lastCompletelyVisibleItemPosition == getItemCount() - 1) return emptyList()

            val firstCompletelyVisibleItemPosition =
                visibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)

            if (lastCompletelyVisibleItemPosition <= firstCompletelyVisibleItemPosition) return emptyList()

            val positions = TreeSet<Int>(kotlin.Comparator { o1, o2 ->
                return@Comparator o2 - o1
            })

            for (position in (lastCompletelyVisibleItemPosition + 1) until getItemCount()) {
                if (isFooterPosition(position)) {
                    positions.add(position)
                }
                if (positions.size >= maxStickyFooters) {
                    break
                }
            }

            for (position in lastCompletelyVisibleItemPosition downTo firstCompletelyVisibleItemPosition) {
                if (isFooterPosition(position)) {
                    val bounds = findItemViewBounds(position) ?: return positions
                    if (positions.size >= maxStickyFooters) {
                        if (bounds.top > (height - itemsHeight)) {
                            positions.remove(positions.first())
                            positions.add(position)
                        }
                    } else {
                        val stickyFooter = attachedItemMap[position]
                        if (stickyFooter != null) {
                            if (bounds.bottom > (height - itemsHeight) + stickyFooter.heightWithDecor) {
                                positions.add(position)
                                if (positions.size >= maxStickyFooters) {
                                    break
                                }
                            }
                        } else {
                            if (bounds.bottom > (height - itemsHeight)) {
                                positions.add(position)
                                if (positions.size >= maxStickyFooters) {
                                    break
                                }
                            }
                        }
                    }
                }
            }
            return positions
        }

        override fun createStickyItem(adapter: RecyclerView.Adapter<*>, position: Int, recycled: StickyItem?): StickyItem {
            return if (recycled != null) {
                createStickyFooter(adapter, position, recycled)
            } else {
                createStickyFooter(adapter, position)
            }
        }

        override fun updateItemsOffsetY() {
            val nextFooterBounds = getNextFooterBounds(lastAttachedItem?.position ?: getItemCount())
            this.itemsOffsetY = if (nextFooterBounds != null
                && nextFooterBounds.bottom > (height - itemsHeight)
            ) {
                (nextFooterBounds.bottom - (height - itemsHeight))
            } else {
                0.0F
            }

            rectPool.recycle(nextFooterBounds)

            firstAttachedItem?.iterator()?.forEach {
                it.updateOffsetY(itemsOffsetY)
            }
        }


        private fun createStickyFooter(
            adapter: RecyclerView.Adapter<*>,
            position: Int
        ): StickyItem {
            val itemViewType = adapter.getItemViewType(position)
            val headerViewHolder: RecyclerView.ViewHolder =
                recyclerView.recycledViewPool.getRecycledView(itemViewType)
                    ?: adapter.onCreateViewHolder(recyclerView, itemViewType)
            return StickyFooter(
                position,
                itemViewType,
                headerViewHolder,
                headerViewHolder.itemView.layoutParams
            ).apply {
                headerViewHolder.itemView.setTag(R.id.sticky_item_tag, this)
            }
        }

        private fun createStickyFooter(
            adapter: RecyclerView.Adapter<*>,
            position: Int,
            recycled: StickyItem,
        ): StickyItem {
            val itemViewType = adapter.getItemViewType(position)
            return StickyFooter(
                position,
                itemViewType,
                recycled.viewHolder,
                recycled.originalLayoutParams
            ).apply {
                viewHolder.itemView.setTag(R.id.sticky_item_tag, this)
            }
        }

        private fun getNextFooterBounds(lastAttachedFooterPosition: Int): Rect? {
            if (attachedItemMap.size < maxStickyFooters) return null
            if (lastAttachedFooterPosition >= getItemCount()) return null
            val nextHeaderPosition = findNextFooterPosition(recyclerView, lastAttachedFooterPosition)
            if (nextHeaderPosition < 0) return null
            val nextHeaderViewHolder =
                recyclerView.findViewHolderForAdapterPosition(nextHeaderPosition) ?: return null

            val stickyItemDecoration = this@StickyItemsLayout.stickyItemDecoration
            return if (stickyItemDecoration == null) {
                rectPool.obtain(
                    nextHeaderViewHolder.itemView.left,
                    nextHeaderViewHolder.itemView.top,
                    nextHeaderViewHolder.itemView.right,
                    nextHeaderViewHolder.itemView.bottom
                )
            } else {
                val outRect = rectPool.obtain()
                stickyItemDecoration.getItemOffsets(outRect, nextHeaderPosition, this@StickyItemsLayout)
                outRect.set(
                    nextHeaderViewHolder.itemView.left - outRect.left,
                    nextHeaderViewHolder.itemView.top - outRect.top,
                    nextHeaderViewHolder.itemView.right + outRect.right,
                    nextHeaderViewHolder.itemView.bottom + outRect.bottom
                )
                outRect
            }
        }

        private fun findNextFooterPosition(recyclerView: RecyclerView, lastFooterPosition: Int): Int {
            val lastVisibleItemPosition =
                visibleItemFinder.findLastVisibleItemPosition(recyclerView.layoutManager)

            val firstCompletelyVisibleItemPosition =
                visibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)

            return when {
                lastVisibleItemPosition == lastFooterPosition -> {
                    findFirstFooterPosition(lastVisibleItemPosition - 1, firstCompletelyVisibleItemPosition)
                }
                lastVisibleItemPosition < lastFooterPosition -> {
                    findFirstFooterPosition(lastVisibleItemPosition, firstCompletelyVisibleItemPosition)
                }
                else -> {
                    findFirstFooterPosition(lastFooterPosition - 1, firstCompletelyVisibleItemPosition)
                }
            }
        }

        private fun findFirstFooterPosition(startPosition: Int, endPosition: Int): Int {
            if (startPosition < endPosition) return RecyclerView.NO_POSITION
            return stickyItemsAdapter.let {
                for (position in startPosition downTo endPosition) {
                    if (it.isStickyFooter(position)) return@let position
                }
                RecyclerView.NO_POSITION
            }
        }

        private fun getItemCount(): Int {
            return recyclerView.adapter?.itemCount ?: 0
        }
    }


    private abstract inner class StickyItem(
        val position: Int,
        val itemViewType: Int,
        val viewHolder: RecyclerView.ViewHolder,
        val originalLayoutParams: ViewGroup.LayoutParams?
    ) {

        val itemView: View
            get() = viewHolder.itemView

        var prev: StickyItem? = null

        var next: StickyItem? = null

        val index: Int
            get() {
                return prev?.let {
                    it.index + 1
                } ?: 0
            }

        private val layoutParams: LayoutParams = if (originalLayoutParams == null) {
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                itemView.layoutParams = this
                adapterPosition = position

            }
        } else {
            LayoutParams(originalLayoutParams).apply {
                itemView.layoutParams = this
                adapterPosition = position
            }
        }

        var stickyItemBound: Boolean = false

        val heightWithDecor: Int
            get() = itemView.measuredHeight + layoutParams.insets.top + layoutParams.insets.bottom

        val offsetY: Float
            get() = itemView.translationY

        fun attach() {
            addView(itemView)
        }

        fun detach() {
            itemView.removeFromParent()
        }

        fun bindItemViewHolder() {
            if (!stickyItemBound) return
            if (!isVisibleItem(position)) return
            val adapter = recyclerView.adapter ?: return
            if (adapter.getItemViewType(position) != itemViewType) return
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return
            adapter.bindViewHolder(viewHolder, position)
            stickyItemBound = false
        }

        fun bindStickyItemViewHolder(force: Boolean = false) {
            if (!force && stickyItemBound) return
            stickyItemsAdapter.onBindStickyViewHolder(viewHolder, position)
            stickyItemBound = true
        }

        abstract fun updateOffsetY(itemsOffsetY: Float)

        private fun View?.removeFromParent() {
            val parent = this?.parent ?: return
            if (parent !is ViewGroup) return
            parent.removeView(this)
        }

        fun iterator(): Iterator<StickyItem> {
            var cursor: StickyItem? = this
            return object : Iterator<StickyItem> {

                override fun hasNext(): Boolean {
                    return cursor != null
                }

                override fun next(): StickyItem {
                    val result = cursor!!
                    cursor = result.next
                    return result
                }
            }
        }

        fun listIterator(): ListIterator<StickyItem> {
            var nextCursor: StickyItem? = this
            var prevCursor: StickyItem? = prev
            return object : ListIterator<StickyItem> {

                override fun hasNext(): Boolean {
                    return nextCursor != null
                }

                override fun hasPrevious(): Boolean {
                    return prevCursor != null
                }

                override fun next(): StickyItem {
                    val result = nextCursor!!
                    prevCursor = result
                    nextCursor = result.next
                    return result
                }

                override fun nextIndex(): Int {
                    return nextCursor!!.index
                }

                override fun previous(): StickyItem {
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

        private fun isVisibleItem(position: Int): Boolean {
            val firstVisibleItemPosition =
                visibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
            val lastVisibleItemPosition =
                visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
            if (firstVisibleItemPosition < 0) return false
            if (firstVisibleItemPosition > lastVisibleItemPosition) return false
            return position in firstVisibleItemPosition..lastVisibleItemPosition
        }
    }


    private inner class StickyHeader(
        position: Int,
        itemViewType: Int,
        viewHolder: RecyclerView.ViewHolder,
        originalLayoutParams: ViewGroup.LayoutParams?
    ) : StickyItem(position, itemViewType, viewHolder, originalLayoutParams) {

        override fun updateOffsetY(itemsOffsetY: Float) {
            if (prev != null) {
                val offsetY = (prev?.offsetY ?: 0.0F) + (prev?.heightWithDecor ?: 0)
                itemView.translationY = offsetY
            } else {
                itemView.translationY = itemsOffsetY
            }
        }
    }

    private inner class StickyFooter(
        position: Int,
        itemViewType: Int,
        viewHolder: RecyclerView.ViewHolder,
        originalLayoutParams: ViewGroup.LayoutParams?
    ) : StickyItem(position, itemViewType, viewHolder, originalLayoutParams) {

        override fun updateOffsetY(itemsOffsetY: Float) {
            if (prev != null) {
                val offsetY = (prev?.offsetY ?: 0.0F) - (prev?.heightWithDecor ?: 0)
                itemView.translationY = offsetY
            } else {
                itemView.translationY = itemsOffsetY
            }
        }
    }

    private class RectPool {

        private val cache = mutableListOf<Rect>()

        fun obtain(left: Int, top: Int, right: Int, bottom: Int): Rect {
            if (cache.isEmpty()) return Rect(left, top, right, bottom)
            return cache.removeAt(0).apply {
                set(left, top, right, bottom)
            }
        }

        fun obtain(): Rect {
            if (cache.isEmpty()) return Rect()
            return cache.removeAt(0)
        }

        fun recycle(rect: Rect?) {
            if (rect == null) return
            rect.set(0, 0, 0, 0)
            if (cache.contains(rect)) return
            cache.add(rect)
        }
    }
}