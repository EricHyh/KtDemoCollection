package com.hyh.list.decoration

import android.graphics.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo
import kotlin.properties.Delegates

abstract class BaseItemSourceFrameDecoration(
    outRect: Rect,
    radius: FloatArray,
    @ColorInt val colorInt: Int
) : RecyclerView.ItemDecoration() {

    private val sourceOutRect: Rect = Rect().apply { set(outRect) }
    private val radiusArray: FloatArray = radius.apply {
        check(this.size == 4) {
            "BaseItemSourceFrameDecoration: radius size must be equal to 4, but now is ${radius.size}"
        }
    }.copyOf()

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
            val itemLocalInfo = adapter.findItemLocalInfo(child, parent) ?: continue
            if (!shouldDrawOver(itemLocalInfo.sourceToken)) return

            parent.getDecoratedBoundsWithMargins(child, itemBoundWithDecoration)

            val itemLeft = itemBoundWithDecoration.left.toFloat() + sourceOutRect.left
            val itemRight = itemBoundWithDecoration.right.toFloat() - sourceOutRect.right
            val itemTopWithDecoration = itemBoundWithDecoration.top + child.translationY

            val itemBottomWithDecoration = itemBoundWithDecoration.bottom + child.translationY

            if (isSourceFirstItem(itemLocalInfo) && isSourceLastItem(itemLocalInfo)) {
                val itemTop = child.top.toFloat()
                val itemBottom = child.bottom.toFloat()
                drawTop(itemTopWithDecoration, itemTop, itemLeft, itemRight, canvas)
                drawBottom(itemBottomWithDecoration, itemBottom, itemRight, itemLeft, canvas)
            } else if (isSourceFirstItem(itemLocalInfo)) {
                val itemTop = child.top.toFloat()
                drawTop(itemTopWithDecoration, itemTop, itemLeft, itemRight, canvas)
            } else if (isSourceLastItem(itemLocalInfo)) {
                val itemBottom = child.bottom.toFloat()
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
        val itemLocalInfo = adapter.findItemLocalInfo(view, parent) ?: return
        if (!shouldDrawOver(itemLocalInfo.sourceToken)) return

        if (isSourceFirstItem(itemLocalInfo) && isSourceLastItem(itemLocalInfo)) {
            outRect.set(sourceOutRect.left, sourceOutRect.top, sourceOutRect.right, sourceOutRect.bottom)
        } else if (isSourceFirstItem(itemLocalInfo)) {
            outRect.set(sourceOutRect.left, sourceOutRect.top, sourceOutRect.right, 0)
        } else if (isSourceLastItem(itemLocalInfo)) {
            outRect.set(sourceOutRect.left, 0, sourceOutRect.right, sourceOutRect.bottom)
        } else {
            outRect.set(sourceOutRect.left, 0, sourceOutRect.right, 0)
        }
    }

    protected abstract fun shouldDrawOver(sourceToken: Any): Boolean

    private fun drawTop(
        itemTopWithDecoration: Float,
        itemTop: Float,
        itemLeft: Float,
        itemRight: Float,
        canvas: Canvas
    ) {
        path.reset()

        path.moveTo(itemLeft, itemTopWithDecoration)
        path.lineTo(itemLeft, itemTop + radiusArray[0])

        arcRectF.set(
            itemLeft,
            itemTop,
            itemLeft + radiusArray[0] * 2,
            itemTop + radiusArray[0] * 2
        )
        path.arcTo(arcRectF, 180F, 90F)

        path.lineTo(itemRight - radiusArray[0], itemTop)

        arcRectF.set(
            itemRight - radiusArray[0] * 2,
            itemTop,
            itemRight,
            itemTop + radiusArray[0] * 2
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
        path.lineTo(itemRight, itemBottom - radiusArray[0])

        arcRectF.set(
            itemRight - radiusArray[0] * 2,
            itemBottom - radiusArray[0] * 2,
            itemRight,
            itemBottom
        )
        path.arcTo(arcRectF, 0F, 90F)

        path.lineTo(itemLeft + radiusArray[0], itemBottom)

        arcRectF.set(
            itemLeft,
            itemBottom - radiusArray[0] * 2,
            itemLeft + radiusArray[0] * 2,
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