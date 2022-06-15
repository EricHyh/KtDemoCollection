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
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 支持 [RecyclerView] 固定顶部与固定底部的布局
 *
 * @author eriche
 * @data 2020/11/30
 */
class StickyItemsLayout : ViewGroup {

    private val maxStickyHeaders
        get() = stickyItemsAdapter?.maxStickyHeaders ?: 0

    private val maxStickyFooters
        get() = stickyItemsAdapter?.maxStickyFooters ?: 0

    private val maxFixedStickyHeaders
        get() = stickyItemsAdapter?.maxFixedStickyHeaders ?: 0

    private val maxFixedStickyFooters
        get() = stickyItemsAdapter?.maxFixedStickyHeaders ?: 0

    private val rectPool = RectPool()

    private val recyclerViewDataObserver = RecyclerViewDataObserver()
    private var cacheAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    private var visibleItemFinder: VisibleItemFinder = DefaultVisibleItemFinder()

    private var recyclerView: RecyclerView? = null
    private var stickyItemsAdapter: IStickyItemsAdapter<RecyclerView.ViewHolder>? = null

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
            updateStickyItem(recyclerView)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            updateStickyItem(recyclerView)
        }
    }


    private fun updateStickyItem(
        recyclerView: RecyclerView,
        scrollState: Int = recyclerView.scrollState,
        dataChanged: Boolean = false
    ) {
        stickyHeaders.beforeUpdateStickyItem()
        stickyFooters.beforeUpdateStickyItem()
        stickyHeaders.updateStickyItem(recyclerView, scrollState, dataChanged)
        stickyFooters.updateStickyItem(recyclerView, scrollState, dataChanged)
        stickyHeaders.afterUpdateStickyItem()
        stickyFooters.afterUpdateStickyItem()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        isChildrenDrawingOrderEnabled = true
    }

    /**
     * 吸顶布局与[RecyclerView]建立联系
     *
     * @param recyclerView 列表控件
     * @param adapter 吸顶布局适配器
     */
    @Suppress("UNCHECKED_CAST")
    fun setup(recyclerView: RecyclerView, adapter: IStickyItemsAdapter<*>) {
        this.recyclerView = recyclerView
        this.stickyItemsAdapter = adapter as IStickyItemsAdapter<RecyclerView.ViewHolder>
        recyclerView.addOnScrollListener(onScrollListener)
        registerAdapterDataObserver(recyclerView)
    }


    /**
     * 吸顶布局装饰器（按需添加）
     *
     * @param decoration
     */
    fun setStickyItemDecoration(decoration: StickyItemDecoration) {
        this.stickyItemDecoration = decoration
        recyclerView?.let {
            updateStickyItem(it, it.scrollState, true)
        }
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

    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
        val size = stickyHeaders.size
        return if (drawingPosition < size) {
            stickyHeaders.getChildDrawingOrder(drawingPosition)
        } else {
            stickyFooters.getChildDrawingOrder(drawingPosition - size)
        } ?: super.getChildDrawingOrder(childCount, drawingPosition)
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
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val recyclerView = this.recyclerView ?: return super.dispatchTouchEvent(ev)

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

        fun findParent(recyclerView: RecyclerView): View? {
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
                        return findParent(recyclerView)?.let {
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
                            it.updateStickyItem(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING)
                            it.updateStickyItem(recyclerView, RecyclerView.SCROLL_STATE_IDLE)
                        }
                    }
                }
                touchedStickyItems = null
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun canScrollVertically(direction: Int): Boolean {
        val recyclerView = this.recyclerView ?: return super.canScrollVertically(direction)
        return super.canScrollVertically(direction) || recyclerView.canScrollVertically(direction)
    }

    private fun measureChildWithMargins(
        child: View,
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
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

    private fun isFixedHeaderPosition(position: Int): Boolean {
        return stickyItemsAdapter?.isFixedStickyHeader(position) ?: false
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return (stickyItemsAdapter?.isStickyHeader(position) ?: false) || isFixedHeaderPosition(position)
    }

    private fun isFixedFooterPosition(position: Int): Boolean {
        return stickyItemsAdapter?.isFixedStickyFooter(position) ?: false
    }

    private fun isFooterPosition(position: Int): Boolean {
        return (stickyItemsAdapter?.isStickyFooter(position) ?: false) || isFixedFooterPosition(position)
    }

    private fun findItemViewBounds(position: Int): Rect? {
        val recyclerView = this.recyclerView ?: return null
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


    private fun forceCalculateHeight(position: Int): Int {
        val recyclerView = this.recyclerView ?: return 0
        val adapter = recyclerView.adapter ?: return 0
        val itemViewType = adapter.getItemViewType(position)
        val viewHolder: RecyclerView.ViewHolder =
            recyclerView.recycledViewPool.getRecycledView(itemViewType)
                ?: adapter.onCreateViewHolder(recyclerView, itemViewType)

        var height = viewHolder.itemView.measuredHeight
        if (height == 0) {
            viewHolder.itemView.measure(0, 0)
            height = viewHolder.itemView.measuredHeight
        }
        val stickyItemDecoration = this.stickyItemDecoration
        return if (stickyItemDecoration == null) {
            height
        } else {
            val outRect = rectPool.obtain()
            stickyItemDecoration.getItemOffsets(outRect, position, this)
            height += (outRect.top + outRect.bottom)
            rectPool.recycle(outRect)
            height
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
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }

        override fun onStateRestorationPolicyChanged() {
            super.onStateRestorationPolicyChanged()
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            recyclerView?.postIdleTask {
                updateStickyItem(this, dataChanged = true)
            }
        }
    }


    private abstract inner class StickyItems {

        protected var firstAttachedItem: StickyItem? = null
        protected var lastAttachedItem: StickyItem? = null

        protected val attachedItemMap: MutableMap<Int, StickyItem> = TreeMap()

        protected val attachedItemTypeMap: MutableMap<Int, MutableList<StickyItem>> = mutableMapOf()

        protected var itemsHeight: Float = 0.0F

        private val drawingOrderItems = mutableListOf<StickyItem>()

        val size: Int
            get() = attachedItemMap.size


        protected abstract val cacheFixedPositions: TreeSet<Int>
        protected abstract val cachePositions: TreeSet<Int>
        protected abstract val tempPositions: TreeSet<Int>

        private var cacheFirstCompletelyVisibleItemPosition: Int? = null
        private var cacheLastCompletelyVisibleItemPosition: Int? = null

        protected abstract val currentItemsPositionHelper: CurrentItemsPositionHelper


        fun getChildDrawingOrder(drawingPosition: Int): Int? {
            return if (drawingPosition in drawingOrderItems.indices) drawingOrderItems[drawingPosition].attachIndexInParent else null
        }

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


        fun beforeUpdateStickyItem() {}

        fun updateStickyItem(
            recyclerView: RecyclerView,
            scrollState: Int = recyclerView.scrollState,
            dataChanged: Boolean = false
        ) {
            val adapter = recyclerView.adapter
            val positions = findCurrentItemsPosition(recyclerView, dataChanged)
            if (adapter == null || positions.isEmpty()) {
                var attachedItem = this.firstAttachedItem
                while (attachedItem != null) {
                    attachedItem.bindItemViewHolder()
                    attachedItem.detach()
                    attachedItem = attachedItem.next
                }
                this.firstAttachedItem = null
                this.lastAttachedItem = null
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
                            stickyItem =
                                createStickyItem(recyclerView, adapter, it, tempOldAttachedItem)
                        } else {
                            stickyItem = createStickyItem(recyclerView, adapter, it)
                            stickyItem.attach()
                            tempOldAttachedItem.detach()
                        }
                    } else {
                        stickyItem = createStickyItem(recyclerView, adapter, it)
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

                removeNext(oldAttachedItem)
                oldAttachedItem?.detach()

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
                        ?: createStickyItem(recyclerView, adapter, it).apply {
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
            post {
                updateItemsOffsetY()
            }
        }

        fun afterUpdateStickyItem() {
            drawingOrderItems.clear()
            attachedItemTypeMap.clear()

            attachedItemMap.values.forEach {
                it.attachIndexInParent = indexOfChild(it.itemView)
                drawingOrderItems.add(it)
                attachedItemTypeMap.getOrPut(it.itemViewType) {
                    mutableListOf<StickyItem>().apply {
                        add(it)
                    }
                }
            }

            drawingOrderItems.sortWith(Comparator { left, right ->
                return@Comparator when {
                    left.isFixedStickyItem && right.isFixedStickyItem -> 0
                    left.isFixedStickyItem && !right.isFixedStickyItem -> 1
                    !left.isFixedStickyItem && right.isFixedStickyItem -> -1
                    !left.isFixedStickyItem && !right.isFixedStickyItem -> 0
                    else -> {
                        0
                    }
                }
            })
        }

        fun findCurrentItemsPosition(
            recyclerView: RecyclerView,
            dataChanged: Boolean
        ): Collection<Int> {
            if (recyclerView.computeVerticalScrollRange() <= 0 || currentItemsPositionHelper.maxStickyItems <= 0) {
                cacheFixedPositions.clear()
                cachePositions.clear()
                tempPositions.clear()
                cacheFirstCompletelyVisibleItemPosition = null
                cacheLastCompletelyVisibleItemPosition = null
                return emptyList()
            }

            val firstCompletelyVisibleItemPosition =
                currentItemsPositionHelper.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)
            if (firstCompletelyVisibleItemPosition == 0) {
                cacheFixedPositions.clear()
                cachePositions.clear()
                tempPositions.clear()
                cacheFirstCompletelyVisibleItemPosition = null
                cacheLastCompletelyVisibleItemPosition = null
                return emptyList()
            }

            val lastCompletelyVisibleItemPosition =
                currentItemsPositionHelper.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
            if (lastCompletelyVisibleItemPosition <= firstCompletelyVisibleItemPosition) {
                cacheFixedPositions.clear()
                cachePositions.clear()
                tempPositions.clear()
                cacheFirstCompletelyVisibleItemPosition = null
                cacheLastCompletelyVisibleItemPosition = null
                return emptyList()
            }


            val cacheFirstCompletelyVisibleItemPosition =
                this.cacheFirstCompletelyVisibleItemPosition
            this.cacheFirstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPosition

            val cacheLastCompletelyVisibleItemPosition = this.cacheLastCompletelyVisibleItemPosition
            this.cacheLastCompletelyVisibleItemPosition = lastCompletelyVisibleItemPosition

            if (!dataChanged) {
                if (cacheFirstCompletelyVisibleItemPosition == firstCompletelyVisibleItemPosition
                    && cacheLastCompletelyVisibleItemPosition == lastCompletelyVisibleItemPosition
                ) {
                    return tempPositions
                }
            }

            val maxFixedStickyHeaders = currentItemsPositionHelper.maxFixedStickyItems
            if (maxFixedStickyHeaders > 0) {
                if (!dataChanged && cacheFixedPositions.isNotEmpty()) {
                    while (cacheFixedPositions.size > maxFixedStickyHeaders ||
                        (cacheFixedPositions.size > 0 && cacheFixedPositions.last() >= firstCompletelyVisibleItemPosition)
                    ) {
                        cacheFixedPositions.pollLast()
                    }
                    if (cacheFixedPositions.size < maxFixedStickyHeaders) {
                        var position = cacheFirstCompletelyVisibleItemPosition
                            ?: cacheFixedPositions.last() + 1
                        while (position < firstCompletelyVisibleItemPosition
                            && cacheFixedPositions.size < maxFixedStickyHeaders
                        ) {
                            if (currentItemsPositionHelper.isFixedStickyItemPosition(position)) {
                                cacheFixedPositions.add(position)
                            }
                            position++
                        }
                    }
                } else {
                    cacheFixedPositions.clear()


                    var position = 0
                    while (position < firstCompletelyVisibleItemPosition
                        && cacheFixedPositions.size < maxFixedStickyHeaders
                    ) {
                        if (currentItemsPositionHelper.isFixedStickyItemPosition(position)) {
                            cacheFixedPositions.add(position)
                        }
                        position++
                    }
                }
            } else {
                cacheFixedPositions.clear()
            }

            val maxStickyHeaders = currentItemsPositionHelper.maxStickyItems - cacheFixedPositions.size

            if (maxStickyHeaders > 0) {
                if (!dataChanged && cachePositions.isNotEmpty()) {
                    var position = if (cacheFirstCompletelyVisibleItemPosition != null) {
                        max(
                            cacheFirstCompletelyVisibleItemPosition,
                            cachePositions.last() + 1
                        )
                    } else {
                        cachePositions.last() + 1
                    }
                    while (position < firstCompletelyVisibleItemPosition) {
                        if (currentItemsPositionHelper.isStickyItemPosition(position)
                            && !currentItemsPositionHelper.isFixedStickyItemPosition(position)
                        ) {
                            cachePositions.add(position)
                        }
                        position++
                    }
                    while ((cachePositions.isNotEmpty() && cachePositions.last() >= firstCompletelyVisibleItemPosition)
                    ) {
                        cachePositions.pollLast()
                    }
                    while (cachePositions.size > maxStickyHeaders) {
                        cachePositions.pollFirst()
                    }
                    if (cachePositions.size < maxStickyHeaders) {
                        position = if (cachePositions.isEmpty()) {
                            firstCompletelyVisibleItemPosition - 1
                        } else {
                            cachePositions.first() - 1
                        }
                        while (position >= 0
                            && cachePositions.size < maxStickyHeaders
                        ) {
                            if (currentItemsPositionHelper.isStickyItemPosition(position)
                                && !currentItemsPositionHelper.isFixedStickyItemPosition(position)
                            ) {
                                cachePositions.add(position)
                            }
                            position--
                        }
                    }
                } else {
                    cachePositions.clear()
                    for (position in (firstCompletelyVisibleItemPosition - 1) downTo 0) {
                        if (currentItemsPositionHelper.isStickyItemPosition(position)
                            && !currentItemsPositionHelper.isFixedStickyItemPosition(position)
                        ) {
                            cachePositions.add(position)
                            if (cachePositions.size >= maxStickyHeaders) {
                                break
                            }
                        }
                    }
                }
            } else {
                cachePositions.clear()
            }

            var isHeadersHeightNotPrepare = false

            for (position in firstCompletelyVisibleItemPosition..lastCompletelyVisibleItemPosition) {
                if (currentItemsPositionHelper.isStickyItemPosition(position)) {
                    val bounds = currentItemsPositionHelper.findItemViewBounds(position) ?: continue
                    val headersHeight = currentItemsPositionHelper.calculateHeight(cacheFixedPositions, cachePositions, dataChanged)
                    if (headersHeight == 0) {
                        isHeadersHeightNotPrepare = true
                    }
                    val top = bounds.top
                    rectPool.recycle(bounds)
                    if (cachePositions.size >= maxStickyHeaders) {
                        val first = cachePositions.firstOrNull() ?: continue
                        val height = currentItemsPositionHelper.calculateHeight(first, dataChanged)
                        if ((headersHeight - top) > height) {
                            cachePositions.remove(first)
                            if (currentItemsPositionHelper.isFixedStickyItemPosition(position)) {
                                cacheFixedPositions.add(position)
                            } else {
                                cachePositions.add(position)
                            }
                        }
                    } else {
                        if (top < headersHeight) {
                            if (currentItemsPositionHelper.isFixedStickyItemPosition(position)) {
                                cacheFixedPositions.add(position)
                            } else {
                                cachePositions.add(position)
                            }
                        }
                    }
                }
            }



            tempPositions.clear()
            currentItemsPositionHelper.fillPositions(tempPositions, cacheFixedPositions, cachePositions)

            if (isHeadersHeightNotPrepare) {
                post {
                    updateStickyItem(recyclerView, dataChanged = dataChanged)
                }
            }

            return tempPositions
        }

        abstract fun createStickyItem(
            recyclerView: RecyclerView,
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

        private fun pointInView(
            view: View,
            localX: Float,
            localY: Float,
            slop: Float = 0.0F
        ): Boolean {
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


        abstract inner class CurrentItemsPositionHelper {

            abstract val maxStickyItems: Int

            abstract val maxFixedStickyItems: Int

            abstract fun findFirstCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int

            abstract fun findLastCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int

            abstract fun isFixedStickyItemPosition(position: Int): Boolean

            abstract fun isStickyItemPosition(position: Int): Boolean

            abstract fun calculateHeight(
                fixedPositions: Collection<Int>,
                positions: Collection<Int>,
                dataChanged: Boolean
            ): Int

            abstract fun calculateHeight(
                position: Int?,
                dataChanged: Boolean
            ): Int

            abstract fun findItemViewBounds(position: Int): Rect?

            abstract fun fillPositions(
                acc: MutableCollection<Int>,
                fixedPositions: Collection<Int>,
                positions: Collection<Int>
            )
        }
    }


    private inner class StickyHeaders : StickyItems() {

        override val cacheFixedPositions = TreeSet<Int>()
        override val cachePositions = TreeSet<Int>()
        override val tempPositions = TreeSet<Int>()

        override val currentItemsPositionHelper: CurrentItemsPositionHelper = object : CurrentItemsPositionHelper() {

            override val maxStickyItems: Int
                get() = maxStickyHeaders

            override val maxFixedStickyItems: Int
                get() = maxFixedStickyHeaders

            override fun findFirstCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
                return visibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView?.layoutManager)
            }

            override fun findLastCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
                return visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView?.layoutManager)
            }

            override fun isFixedStickyItemPosition(position: Int): Boolean {
                return isFixedHeaderPosition(position)
            }

            override fun isStickyItemPosition(position: Int): Boolean {
                return isHeaderPosition(position)
            }

            override fun calculateHeight(fixedPositions: Collection<Int>, positions: Collection<Int>, dataChanged: Boolean): Int {
                var height = 0
                if (!dataChanged) {
                    fixedPositions.forEach {
                        val stickyItem = attachedItemMap[it]
                        if (stickyItem != null) {
                            height += stickyItem.heightWithDecor
                        } else {
                            val bounds = findItemViewBounds(it)
                            if (bounds != null) {
                                height += (bounds.bottom - bounds.top)
                                rectPool.recycle(bounds)
                            } else {
                                height += forceCalculateHeight(it)
                            }
                        }
                    }
                    positions.forEach {
                        val stickyItem = attachedItemMap[it]
                        if (stickyItem != null) {
                            height += stickyItem.heightWithDecor
                        } else {
                            val bounds = findItemViewBounds(it)
                            if (bounds != null) {
                                height += (bounds.bottom - bounds.top)
                                rectPool.recycle(bounds)
                            } else {
                                height += forceCalculateHeight(it)
                            }
                        }
                    }
                } else {
                    fixedPositions.forEach {
                        height += calculateHeight(it, dataChanged)
                    }
                    positions.forEach {
                        height += calculateHeight(it, dataChanged)
                    }
                }
                return height
            }

            override fun calculateHeight(position: Int?, dataChanged: Boolean): Int {
                position ?: return 0
                if (dataChanged) {
                    val recyclerView = recyclerView ?: return 0
                    val adapter = recyclerView.adapter ?: return 0
                    val itemViewType = adapter.getItemViewType(position)
                    val attachedItem = attachedItemTypeMap[itemViewType]?.firstOrNull()
                    if (attachedItem != null) {
                        return attachedItem.heightWithDecor
                    }
                    return forceCalculateHeight(position)
                }
                val stickyItem = attachedItemMap[position]
                if (stickyItem != null) {
                    return stickyItem.heightWithDecor
                }
                val bounds = findItemViewBounds(position) ?: return forceCalculateHeight(position)
                val height = bounds.bottom - bounds.top
                rectPool.recycle(bounds)
                return height
            }

            override fun findItemViewBounds(position: Int): Rect? {
                return this@StickyItemsLayout.findItemViewBounds(position)
            }

            override fun fillPositions(
                acc: MutableCollection<Int>,
                fixedPositions: Collection<Int>,
                positions: Collection<Int>
            ) {
                acc.apply {
                    addAll(cacheFixedPositions)
                    addAll(cachePositions)
                }
            }
        }

        override fun onLayout() {
            firstAttachedItem?.iterator()?.forEach {
                val child = it.itemView
                val layoutParams = child.layoutParams as LayoutParams
                val childLeft: Int =
                    paddingLeft + layoutParams.leftMargin + layoutParams.insets.left
                val childTop: Int = paddingTop + layoutParams.topMargin + layoutParams.insets.top
                val childRight: Int = childLeft + child.measuredWidth
                val childBottom: Int = childTop + child.measuredHeight
                child.layout(childLeft, childTop, childRight, childBottom)
            }
        }

        override fun createStickyItem(
            recyclerView: RecyclerView,
            adapter: RecyclerView.Adapter<*>,
            position: Int,
            recycled: StickyItem?
        ): StickyItem {
            return if (recycled != null) {
                createStickyHeader(adapter, position, recycled)
            } else {
                createStickyHeader(recyclerView, adapter, position)
            }
        }

        override fun updateItemsOffsetY() {
            val nextHeaderBounds = getNextHeaderBounds(lastAttachedItem?.position ?: -1)
            var itemsOffsetY = if (nextHeaderBounds != null
                && nextHeaderBounds.top < itemsHeight
            ) {
                (nextHeaderBounds.top - itemsHeight)
            } else {
                0.0F
            }
            rectPool.recycle(nextHeaderBounds)
            firstAttachedItem?.iterator()?.forEach {
                itemsOffsetY = it.updateOffsetY(itemsOffsetY)
            }
        }


        private fun createStickyHeader(
            recyclerView: RecyclerView,
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
            val recyclerView = this@StickyItemsLayout.recyclerView ?: return null
            if (attachedItemMap.size < maxStickyHeaders) return null
            if (lastAttachedHeaderPosition < 0) return null
            val nextHeaderPosition =
                findNextHeaderPosition(recyclerView, lastAttachedHeaderPosition)
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
                stickyItemDecoration.getItemOffsets(
                    outRect,
                    nextHeaderPosition,
                    this@StickyItemsLayout
                )
                outRect.set(
                    nextHeaderViewHolder.itemView.left - outRect.left,
                    nextHeaderViewHolder.itemView.top - outRect.top,
                    nextHeaderViewHolder.itemView.right + outRect.right,
                    nextHeaderViewHolder.itemView.bottom + outRect.bottom
                )
                outRect
            }
        }

        private fun findNextHeaderPosition(
            recyclerView: RecyclerView,
            lastHeaderPosition: Int
        ): Int {
            val firstVisibleItemPosition =
                visibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
            val lastCompletelyVisibleItemPosition =
                visibleItemFinder.findLastCompletelyVisibleItemPosition(recyclerView.layoutManager)
            return when {
                firstVisibleItemPosition == lastHeaderPosition -> {
                    findFirstHeaderPosition(
                        firstVisibleItemPosition + 1,
                        lastCompletelyVisibleItemPosition
                    )
                }
                firstVisibleItemPosition > lastHeaderPosition -> {
                    findFirstHeaderPosition(firstVisibleItemPosition, lastCompletelyVisibleItemPosition)
                }
                else -> {
                    findFirstHeaderPosition(lastHeaderPosition + 1, lastCompletelyVisibleItemPosition)
                }
            }
        }

        private fun findFirstHeaderPosition(startPosition: Int, endPosition: Int): Int {
            if (startPosition > endPosition) return RecyclerView.NO_POSITION
            return stickyItemsAdapter?.let {
                for (position in startPosition..endPosition) {
                    if (it.isStickyHeader(position) || it.isFixedStickyHeader(position)) return@let position
                }
                RecyclerView.NO_POSITION
            } ?: RecyclerView.NO_POSITION
        }
    }


    private inner class StickyFooters : StickyItems() {


        override val cacheFixedPositions: TreeSet<Int> = TreeSet()

        override val cachePositions: TreeSet<Int> = TreeSet()

        override val tempPositions: TreeSet<Int> = TreeSet(kotlin.Comparator { o1, o2 ->
            return@Comparator o2 - o1
        })

        override val currentItemsPositionHelper: CurrentItemsPositionHelper = object : CurrentItemsPositionHelper() {

            override val maxStickyItems: Int
                get() = maxStickyFooters

            override val maxFixedStickyItems: Int
                get() = maxFixedStickyFooters

            override fun findFirstCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
                val position = visibleItemFinder.findLastCompletelyVisibleItemPosition(layoutManager)
                if (position == RecyclerView.NO_POSITION) return position
                return getRealPosition(position)
            }

            override fun findLastCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
                val position = visibleItemFinder.findFirstCompletelyVisibleItemPosition(layoutManager)
                if (position == RecyclerView.NO_POSITION) return position
                return getRealPosition(position)
            }

            override fun isFixedStickyItemPosition(position: Int): Boolean {
                return isFixedFooterPosition(getRealPosition(position))
            }

            override fun isStickyItemPosition(position: Int): Boolean {
                return isFooterPosition(getRealPosition(position))
            }

            override fun calculateHeight(fixedPositions: Collection<Int>, positions: Collection<Int>, dataChanged: Boolean): Int {
                var height = 0
                if (!dataChanged) {
                    fixedPositions.forEach {
                        val stickyItem = attachedItemMap[getRealPosition(it)]
                        if (stickyItem != null) {
                            height += stickyItem.heightWithDecor
                        } else {
                            val bounds = findItemViewBounds(getRealPosition(it))
                            if (bounds != null) {
                                height += (bounds.bottom - bounds.top)
                                rectPool.recycle(bounds)
                            } else {
                                height += forceCalculateHeight(getRealPosition(it))
                            }
                        }
                    }
                    positions.forEach {
                        val stickyItem = attachedItemMap[getRealPosition(it)]
                        if (stickyItem != null) {
                            height += stickyItem.heightWithDecor
                        } else {
                            val bounds = findItemViewBounds(getRealPosition(it))
                            if (bounds != null) {
                                height += (bounds.bottom - bounds.top)
                                rectPool.recycle(bounds)
                            } else {
                                height += forceCalculateHeight(getRealPosition(it))
                            }
                        }
                    }
                } else {
                    fixedPositions.forEach {
                        height += calculateHeight(it, dataChanged)
                    }
                    positions.forEach {
                        height += calculateHeight(it, dataChanged)
                    }
                }
                return height
            }

            override fun calculateHeight(position: Int?, dataChanged: Boolean): Int {
                position ?: return 0
                if (dataChanged) {
                    val recyclerView = recyclerView ?: return 0
                    val adapter = recyclerView.adapter ?: return 0
                    val itemViewType = adapter.getItemViewType(getRealPosition(position))
                    val attachedItem = attachedItemTypeMap[itemViewType]?.firstOrNull()
                    if (attachedItem != null) {
                        return attachedItem.heightWithDecor
                    }
                    return forceCalculateHeight(getRealPosition(position))
                }
                val stickyItem = attachedItemMap[getRealPosition(position)]
                if (stickyItem != null) {
                    return stickyItem.heightWithDecor
                }
                val bounds = findItemViewBounds(getRealPosition(position)) ?: return forceCalculateHeight(getRealPosition(position))
                val height = bounds.bottom - bounds.top
                rectPool.recycle(bounds)
                return height
            }

            override fun findItemViewBounds(position: Int): Rect? {
                val bounds = this@StickyItemsLayout.findItemViewBounds(getRealPosition(position)) ?: return null
                val height = this@StickyItemsLayout.height
                val top = height - bounds.bottom
                val bottom = height - bounds.top
                return bounds.apply {
                    this.top = top
                    this.bottom = bottom
                }
            }

            override fun fillPositions(
                acc: MutableCollection<Int>,
                fixedPositions: Collection<Int>,
                positions: Collection<Int>
            ) {
                fixedPositions.forEach {
                    acc.add(getRealPosition(it))
                }
                positions.forEach {
                    acc.add(getRealPosition(it))
                }
            }

            private fun getRealPosition(position: Int): Int {
                return getItemCount() - position - 1
            }
        }


        override fun onLayout() {
            firstAttachedItem?.iterator()?.forEach {
                val child = it.itemView
                val layoutParams = child.layoutParams as LayoutParams
                val childLeft: Int =
                    paddingLeft + layoutParams.leftMargin + layoutParams.insets.left
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

        override fun createStickyItem(
            recyclerView: RecyclerView,
            adapter: RecyclerView.Adapter<*>,
            position: Int,
            recycled: StickyItem?
        ): StickyItem {
            return if (recycled != null) {
                createStickyFooter(adapter, position, recycled)
            } else {
                createStickyFooter(recyclerView, adapter, position)
            }
        }

        override fun updateItemsOffsetY() {
            val nextFooterBounds = getNextFooterBounds(lastAttachedItem?.position ?: getItemCount())
            var itemsOffsetY = if (nextFooterBounds != null
                && nextFooterBounds.bottom > (height - itemsHeight)
            ) {
                (nextFooterBounds.bottom - (height - itemsHeight))
            } else {
                0.0F
            }

            rectPool.recycle(nextFooterBounds)

            firstAttachedItem?.iterator()?.forEach {
                itemsOffsetY = it.updateOffsetY(itemsOffsetY)
            }
        }


        private fun createStickyFooter(
            recyclerView: RecyclerView,
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
            val recyclerView = this@StickyItemsLayout.recyclerView ?: return null
            if (attachedItemMap.size < maxStickyFooters) return null
            if (lastAttachedFooterPosition >= getItemCount()) return null
            val nextHeaderPosition =
                findNextFooterPosition(recyclerView, lastAttachedFooterPosition)
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
                stickyItemDecoration.getItemOffsets(
                    outRect,
                    nextHeaderPosition,
                    this@StickyItemsLayout
                )
                outRect.set(
                    nextHeaderViewHolder.itemView.left - outRect.left,
                    nextHeaderViewHolder.itemView.top - outRect.top,
                    nextHeaderViewHolder.itemView.right + outRect.right,
                    nextHeaderViewHolder.itemView.bottom + outRect.bottom
                )
                outRect
            }
        }

        private fun findNextFooterPosition(
            recyclerView: RecyclerView,
            lastFooterPosition: Int
        ): Int {
            val lastVisibleItemPosition =
                visibleItemFinder.findLastVisibleItemPosition(recyclerView.layoutManager)

            val firstCompletelyVisibleItemPosition =
                visibleItemFinder.findFirstCompletelyVisibleItemPosition(recyclerView.layoutManager)

            return when {
                lastVisibleItemPosition == lastFooterPosition -> {
                    findFirstFooterPosition(
                        lastVisibleItemPosition - 1,
                        firstCompletelyVisibleItemPosition
                    )
                }
                lastVisibleItemPosition < lastFooterPosition -> {
                    findFirstFooterPosition(
                        lastVisibleItemPosition,
                        firstCompletelyVisibleItemPosition
                    )
                }
                else -> {
                    findFirstFooterPosition(
                        lastFooterPosition - 1,
                        firstCompletelyVisibleItemPosition
                    )
                }
            }
        }

        private fun findFirstFooterPosition(startPosition: Int, endPosition: Int): Int {
            if (startPosition < endPosition) return RecyclerView.NO_POSITION
            return stickyItemsAdapter?.let {
                for (position in startPosition downTo endPosition) {
                    if (it.isStickyFooter(position)) return@let position
                }
                RecyclerView.NO_POSITION
            } ?: RecyclerView.NO_POSITION
        }

        private fun getItemCount(): Int {
            return recyclerView?.adapter?.itemCount ?: 0
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

        abstract val isFixedStickyItem: Boolean

        var attachIndexInParent: Int = -1

        private val layoutParams: LayoutParams = if (originalLayoutParams == null) {
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
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
            val childCount = childCount
            addView(itemView)
            attachIndexInParent = childCount
        }

        fun detach() {
            itemView.removeFromParent()
            attachIndexInParent = -1
        }

        fun bindItemViewHolder() {
            val recyclerView = this@StickyItemsLayout.recyclerView ?: return
            if (!stickyItemBound) return
            if (!isVisibleItem(position)) return
            val adapter = recyclerView.adapter ?: return
            if (adapter.getItemViewType(position) != itemViewType) return
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return
            adapter.bindViewHolder(viewHolder, position)
            stickyItemBound = false
        }

        fun bindStickyItemViewHolder(force: Boolean = false) {
            val stickyItemsAdapter = this@StickyItemsLayout.stickyItemsAdapter ?: return
            if (!force && stickyItemBound) return
            stickyItemsAdapter.onBindStickyViewHolder(viewHolder, position)
            stickyItemBound = true
        }

        abstract fun updateOffsetY(itemsOffsetY: Float): Float

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
            val recyclerView = this@StickyItemsLayout.recyclerView ?: return false
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

        override val isFixedStickyItem: Boolean = isFixedHeaderPosition(position)

        override fun updateOffsetY(itemsOffsetY: Float): Float {
            val isFixedStickyItem = isFixedStickyItem
            var consumed = 0.0F
            if (prev != null) {
                if (prev?.isFixedStickyItem == true) {
                    if (isFixedStickyItem) {
                        itemView.translationY = (prev?.offsetY ?: 0.0F) + (prev?.heightWithDecor?.toFloat() ?: 0.0F)
                    } else {
                        val offsetY =
                            (prev?.offsetY ?: 0.0F) + (prev?.heightWithDecor ?: 0) + itemsOffsetY
                        itemView.translationY = offsetY
                        consumed = itemsOffsetY
                    }
                } else {
                    val offsetY = (prev?.offsetY ?: 0.0F) + (prev?.heightWithDecor ?: 0)
                    itemView.translationY = offsetY
                }
            } else {
                if (isFixedStickyItem) {
                    itemView.translationY = 0.0F
                } else {
                    itemView.translationY = itemsOffsetY
                    consumed = itemsOffsetY
                }
            }
            return itemsOffsetY - consumed
        }
    }

    private inner class StickyFooter(
        position: Int,
        itemViewType: Int,
        viewHolder: RecyclerView.ViewHolder,
        originalLayoutParams: ViewGroup.LayoutParams?
    ) : StickyItem(position, itemViewType, viewHolder, originalLayoutParams) {

        override val isFixedStickyItem: Boolean
            get() = isFixedFooterPosition(position)

        override fun updateOffsetY(itemsOffsetY: Float): Float {
            val isFixedStickyItem = isFixedStickyItem
            var consumed = 0.0F
            if (prev != null) {
                if (prev?.isFixedStickyItem == true) {
                    if (isFixedStickyItem) {
                        itemView.translationY = (prev?.offsetY ?: 0.0F) - (prev?.heightWithDecor?.toFloat() ?: 0.0F)
                    } else {
                        val offsetY = (prev?.offsetY ?: 0.0F) - (prev?.heightWithDecor ?: 0) + itemsOffsetY
                        itemView.translationY = offsetY
                        consumed = itemsOffsetY
                    }
                } else {
                    val offsetY = (prev?.offsetY ?: 0.0F) - (prev?.heightWithDecor ?: 0)
                    itemView.translationY = offsetY
                }
            } else {
                if (isFixedStickyItem) {
                    itemView.translationY = 0.0F
                } else {
                    itemView.translationY = itemsOffsetY
                    consumed = itemsOffsetY
                }
            }
            return itemsOffsetY - consumed
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