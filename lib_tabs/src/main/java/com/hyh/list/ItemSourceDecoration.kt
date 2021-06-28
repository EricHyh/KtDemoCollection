package com.hyh.list

import android.graphics.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo
import kotlin.properties.Delegates

class ItemSourceDecoration private constructor() : RecyclerView.ItemDecoration() {


    private var supportedSources: List<Any>? = null
    private val sourceOutRect: Rect = Rect()
    private val radius: FloatArray = FloatArray(4)
    private var colorInt by Delegates.notNull<Int>()

    constructor(padding: Int, radius: Float, @ColorInt colorInt: Int, supportedSources: List<Any>? = null)
            : this(Rect().apply { set(padding, padding, padding, padding) }, radius, colorInt, supportedSources)

    constructor(outRect: Rect, radius: Float, @ColorInt colorInt: Int, supportedSources: List<Any>? = null)
            : this(outRect, FloatArray(4) { radius }, colorInt, supportedSources)


    constructor(outRect: Rect, radius: FloatArray, @ColorInt colorInt: Int, supportedSources: List<Any>? = null) : this() {
        this.sourceOutRect.set(outRect)
        for (index in 0 until 4) {
            this.radius[index] = radius[index]
        }
        this.colorInt = colorInt
        this.supportedSources = supportedSources
    }


    private val itemBoundWithDecoration = Rect()
    private val arcRectF = RectF()
    private val tempRect = Rect()

    private val path = Path()

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        if (adapter !is IListAdapter<*>) return
        canvas.save()
        val childCount = parent.childCount
        for (index in 0 until childCount) {
            val child = parent.getChildAt(index)
            val itemLocalInfo = adapter.findItemLocalInfo(child, parent)
            parent.getDecoratedBoundsWithMargins(child, itemBoundWithDecoration)

            val itemHeight = itemBoundWithDecoration.bottom - itemBoundWithDecoration.top
            val outHeight = itemHeight - child.height


            val itemLeft = itemBoundWithDecoration.left.toFloat() + sourceOutRect.left
            val itemRight = itemBoundWithDecoration.right.toFloat() - sourceOutRect.right
            val itemTopWithDecoration = itemBoundWithDecoration.top + child.translationY

            val itemBottomWithDecoration = itemBoundWithDecoration.bottom + child.translationY

            if (isSourceFirstItem(itemLocalInfo) && isSourceLastItem(itemLocalInfo)) {
                val itemTop = child.top.toFloat()
                val itemBottom = child.bottom.toFloat()
                /*if (outHeight > sourceOutRect.top) {
                    itemTop = itemTopWithDecoration + sourceOutRect.top
                    itemBottom = itemBottomWithDecoration - sourceOutRect.bottom
                } else if (outHeight == 0) {
                    itemTop = itemTopWithDecoration
                    itemBottom = itemBottomWithDecoration
                } else {
                    if (sourceOutRect.top == sourceOutRect.bottom) {



                    } else if (sourceOutRect.top == outHeight) {
                        itemTop = itemTopWithDecoration + sourceOutRect.top
                        itemBottom = itemBottomWithDecoration
                    } else {
                        itemTop = itemTopWithDecoration
                        itemBottom = itemBottomWithDecoration - sourceOutRect.bottom
                    }
                }*/
                drawTop(itemTopWithDecoration, itemTop, itemLeft, itemRight, canvas)
                drawBottom(itemBottomWithDecoration, itemBottom, itemRight, itemLeft, canvas)
            } else if (isSourceFirstItem(itemLocalInfo)) {
                val itemTop = child.top.toFloat()
                //val itemTop = itemTopWithDecoration + outHeight
                drawTop(itemTopWithDecoration, itemTop, itemLeft, itemRight, canvas)
            } else if (isSourceLastItem(itemLocalInfo)) {
                val itemBottom = child.bottom.toFloat()
                //val itemBottom = itemBottomWithDecoration - outHeight
                drawBottom(itemBottomWithDecoration, itemBottom, itemRight, itemLeft, canvas)
            }

            tempRect.set(
                itemBoundWithDecoration.left,
                itemBoundWithDecoration.top,
                sourceOutRect.left,
                itemBoundWithDecoration.bottom
            )
            canvas.drawRect(tempRect, paint)

            tempRect.set(
                itemBoundWithDecoration.right - sourceOutRect.right,
                itemBoundWithDecoration.top,
                itemBoundWithDecoration.right,
                itemBoundWithDecoration.bottom
            )
            canvas.drawRect(tempRect, paint)

        }
        canvas.restore()
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        if (adapter !is IListAdapter<*>) return
        val itemLocalInfo = adapter.findItemLocalInfo(view, parent)
        if (isSourceFirstItem(itemLocalInfo) && isSourceLastItem(itemLocalInfo)) {
            outRect.set(sourceOutRect.left, sourceOutRect.top, sourceOutRect.right, sourceOutRect.bottom)
        } else if (isSourceFirstItem(itemLocalInfo)) {
            outRect.set(sourceOutRect.left, sourceOutRect.top, sourceOutRect.right, 0)
        } else if ( isSourceLastItem(itemLocalInfo)) {
            outRect.set(sourceOutRect.left, 0, sourceOutRect.right, sourceOutRect.bottom)
        } else {
            outRect.set(sourceOutRect.left, 0, sourceOutRect.right, 0)
        }
    }

    private fun drawTop(
        itemTopWithDecoration: Float,
        itemTop: Float,
        itemLeft: Float,
        itemRight: Float,
        canvas: Canvas
    ) {
        path.reset()

        path.moveTo(itemLeft, itemTopWithDecoration)
        path.lineTo(itemLeft, itemTop + radius[0])

        arcRectF.set(
            itemLeft,
            itemTop,
            itemLeft + radius[0] * 2,
            itemTop + radius[0] * 2
        )
        path.arcTo(arcRectF, 180F, 90F)

        path.lineTo(itemRight - radius[0], itemTop)

        arcRectF.set(
            itemRight - radius[0] * 2,
            itemTop,
            itemRight,
            itemTop + radius[0] * 2
        )
        path.arcTo(arcRectF, 270F, 90F)

        path.lineTo(itemRight, itemTopWithDecoration)
        path.close()

        canvas.drawPath(path, paint)
    }

    private fun drawBottom(
        itemBottomWithDecoration: Float,
        itemBottom: Float,
        itemRight: Float,
        itemLeft: Float,
        canvas: Canvas
    ) {
        path.reset()

        path.moveTo(itemRight, itemBottomWithDecoration)
        path.lineTo(itemRight, itemBottom - radius[0])

        arcRectF.set(
            itemRight - radius[0] * 2,
            itemBottom - radius[0] * 2,
            itemRight,
            itemBottom
        )
        path.arcTo(arcRectF, 0F, 90F)

        path.lineTo(itemLeft + radius[0], itemBottom)

        arcRectF.set(
            itemLeft,
            itemBottom - radius[0] * 2,
            itemLeft + radius[0] * 2,
            itemBottom
        )
        path.arcTo(arcRectF, 90F, 90F)

        path.lineTo(itemLeft, itemBottomWithDecoration)

        path.close()

        canvas.drawPath(path, paint)
    }

    private fun isSourceFirstItem(itemLocalInfo: ItemLocalInfo?): Boolean {
        if (itemLocalInfo == null) return false
        return itemLocalInfo.localPosition == 0
    }

    private fun isSourceLastItem(itemLocalInfo: ItemLocalInfo?): Boolean {
        if (itemLocalInfo == null) return false
        return itemLocalInfo.localPosition == itemLocalInfo.sourceItemCount - 1
    }
}