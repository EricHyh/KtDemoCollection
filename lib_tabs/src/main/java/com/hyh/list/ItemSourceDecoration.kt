package com.hyh.list

import android.graphics.*
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemSourceDecoration : RecyclerView.ItemDecoration() {

    private val bounds = Rect()

    private val rectf = RectF()
    private val rect = Rect()

    private val path = Path()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }


    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        canvas.save()
        val childCount = parent.childCount
        for (index in 0 until childCount) {
            val child = parent.getChildAt(index)
            val childAdapterPosition = parent.getChildAdapterPosition(child)
            parent.getDecoratedBoundsWithMargins(child, bounds)
            if (childAdapterPosition == 0) {
                path.reset()

                val itemLeft = bounds.left.toFloat() + 40
                val itemRight = bounds.right.toFloat() - 40
                val itemTop = bounds.top + child.translationY


                path.moveTo(itemLeft, itemTop)
                path.lineTo(itemLeft, itemTop + 60)

                rectf.set(itemLeft, itemTop + 40, itemLeft + 40F, itemTop + 80)
                path.arcTo(rectf, 180F, 90F)

                path.lineTo(itemRight - 20F, itemTop + 40)

                rectf.set(itemRight - 40, itemTop + 40, itemRight, itemTop + 80)
                path.arcTo(rectf, 270F, 90F)

                path.lineTo(itemRight, itemTop)
                path.close()

                canvas.drawPath(path, paint)

            }

            if (childAdapterPosition == adapter.itemCount - 1) {
                path.reset()


                val itemLeft = bounds.left.toFloat() + 40
                val itemRight = bounds.right.toFloat() - 40
                val itemBottom = bounds.bottom + child.translationY

                val itemHeight = bounds.bottom - bounds.top
                val outTopAndBottom = itemHeight - child.height
                val outBottom = if (childAdapterPosition == 0) {
                    outTopAndBottom / 2
                } else {
                    outTopAndBottom
                }


                path.moveTo(itemRight, itemBottom)
                path.lineTo(itemRight, itemBottom - outBottom - 20)

                rectf.set(itemRight - 40, itemBottom - outBottom - 40, itemRight, itemBottom - outBottom)
                path.arcTo(rectf, 0F, 90F)

                path.lineTo(itemRight - 20, itemBottom - outBottom)

                rectf.set(itemLeft, itemBottom - outBottom - 40, itemLeft + 40F, itemBottom - outBottom)
                path.arcTo(rectf, 90F, 90F)

                path.lineTo(itemLeft, itemBottom)

                path.close()

                canvas.drawPath(path, paint)
            }

            rect.set(bounds.left, bounds.top, 40, bounds.bottom)
            canvas.drawRect(rect, paint)

            rect.set(bounds.right - 40, bounds.top, bounds.right, bounds.bottom)
            canvas.drawRect(rect, paint)

        }
        canvas.restore()
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        val childAdapterPosition = parent.getChildAdapterPosition(view)
        if (childAdapterPosition == 0 && childAdapterPosition == adapter.itemCount - 1) {
            outRect.set(40, 40, 40, 40)
        } else if (childAdapterPosition == 0) {
            outRect.set(40, 40, 40, 0)
        } else if (childAdapterPosition == adapter.itemCount - 1) {
            outRect.set(40, 0, 40, 40)
        } else {
            outRect.set(40, 0, 40, 0)
        }
    }
}