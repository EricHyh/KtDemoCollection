package com.hyh.list.decoration

import android.graphics.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.adapter.IListAdapter
import com.hyh.list.adapter.ItemLocalInfo
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

abstract class BaseRoundCardDecoration(
    @ColorInt val colorInt: Int
) : RecyclerView.ItemDecoration() {

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
            val cardPosition = getCardPosition(adapter, itemLocalInfo) ?: continue

            parent.getDecoratedBoundsWithMargins(child, itemBoundWithDecoration)

            val itemLeft = child.left.toFloat()
            val itemRight = child.right.toFloat()
            val itemTopWithDecoration = itemBoundWithDecoration.top + child.translationY

            val itemBottomWithDecoration = itemBoundWithDecoration.bottom + child.translationY

            when (cardPosition) {
                is CardPosition.Whole -> {
                    val itemTop = child.top.toFloat()
                    val itemBottom = child.bottom.toFloat()
                    drawTop(
                        itemTopWithDecoration, itemTop, itemLeft, itemRight,
                        cardPosition.corners[0], cardPosition.corners[1],
                        canvas
                    )
                    drawBottom(
                        itemBottomWithDecoration, itemBottom, itemRight, itemLeft,
                        cardPosition.corners[2], cardPosition.corners[3],
                        canvas
                    )
                }
                is CardPosition.Top -> {
                    val itemTop = child.top.toFloat()
                    drawTop(
                        itemTopWithDecoration, itemTop, itemLeft, itemRight,
                        cardPosition.leftCorner, cardPosition.rightCorner,
                        canvas
                    )
                }
                is CardPosition.Bottom -> {
                    val itemBottom = child.bottom.toFloat()
                    drawBottom(
                        itemBottomWithDecoration, itemBottom, itemRight, itemLeft,
                        cardPosition.leftCorner, cardPosition.rightCorner,
                        canvas
                    )
                }
                else -> {
                }
            }

            tempRect.set(
                itemBoundWithDecoration.left,
                itemBoundWithDecoration.top,
                child.left,
                itemBoundWithDecoration.bottom
            )
            canvas.drawRect(tempRect, paint)

            tempRect.set(
                child.right,
                itemBoundWithDecoration.top,
                itemBoundWithDecoration.right,
                itemBoundWithDecoration.bottom
            )
            canvas.drawRect(tempRect, paint)

        }
        canvas.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val adapter = parent.adapter ?: return
        if (adapter !is IListAdapter<*>) return
        val itemLocalInfo = adapter.findItemLocalInfo(view, parent) ?: return
        getItemOffsets(outRect, adapter, itemLocalInfo)
    }

    protected abstract fun getCardPosition(
        adapter: IListAdapter<*>,
        itemLocalInfo: ItemLocalInfo
    ): CardPosition?


    protected abstract fun getItemOffsets(
        outRect: Rect,
        adapter: IListAdapter<*>,
        itemLocalInfo: ItemLocalInfo
    )

    private fun drawTop(
        itemTopWithDecoration: Float,
        itemTop: Float,
        itemLeft: Float,
        itemRight: Float,
        topLiftCorner: RoundCorner,
        topRightCorner: RoundCorner,
        canvas: Canvas
    ) {

        path.reset()

        path.moveTo(itemLeft, itemTopWithDecoration)
        path.lineTo(itemLeft, itemTop + topLiftCorner.radius)

        if (topLiftCorner.direction == RoundCorner.DIRECTION_OUT) {
            arcRectF.set(
                itemLeft,
                itemTop,
                itemLeft + topLiftCorner.radius * 2,
                itemTop + topLiftCorner.radius * 2
            )

            path.arcTo(arcRectF, 180F, 90F)

        } else {
            arcRectF.set(
                itemLeft - topLiftCorner.radius,
                itemTop - topLiftCorner.radius,
                itemLeft + topLiftCorner.radius,
                itemTop + topLiftCorner.radius
            )

            path.arcTo(arcRectF, 90F, -90F)
        }

        path.lineTo(itemRight - topRightCorner.radius, itemTop)

        if (topRightCorner.direction == RoundCorner.DIRECTION_OUT) {
            arcRectF.set(
                itemRight - topRightCorner.radius * 2,
                itemTop,
                itemRight,
                itemTop + topRightCorner.radius * 2
            )
            path.arcTo(arcRectF, 270F, 90F)
        } else {
            arcRectF.set(
                itemRight - topRightCorner.radius,
                itemTop - topRightCorner.radius,
                itemRight + topRightCorner.radius,
                itemTop + topRightCorner.radius
            )
            path.arcTo(arcRectF, 180F, -90F)
        }

        path.lineTo(itemRight, itemTopWithDecoration)
        path.close()

        canvas.drawPath(path, paint)
    }

    private fun drawBottom(
        itemBottomWithDecoration: Float,
        itemBottom: Float,
        itemRight: Float,
        itemLeft: Float,
        bottomLeftCorner: RoundCorner,
        bottomRightCorner: RoundCorner,
        canvas: Canvas
    ) {
        path.reset()


        path.moveTo(itemRight, itemBottomWithDecoration)
        path.lineTo(itemRight, itemBottom - bottomRightCorner.radius)

        if (bottomRightCorner.direction == RoundCorner.DIRECTION_OUT) {
            arcRectF.set(
                itemRight - bottomRightCorner.radius * 2,
                itemBottom - bottomRightCorner.radius * 2,
                itemRight,
                itemBottom
            )
            path.arcTo(arcRectF, 0F, 90F)
        } else {
            arcRectF.set(
                itemRight - bottomRightCorner.radius,
                itemBottom - bottomRightCorner.radius,
                itemRight + bottomRightCorner.radius,
                itemBottom + bottomRightCorner.radius
            )
            path.arcTo(arcRectF, 270F, -90F)
        }

        path.lineTo(itemLeft + bottomLeftCorner.radius, itemBottom)

        if (bottomLeftCorner.direction == RoundCorner.DIRECTION_OUT) {
            arcRectF.set(
                itemLeft,
                itemBottom - bottomLeftCorner.radius * 2,
                itemLeft + bottomLeftCorner.radius * 2,
                itemBottom
            )
            path.arcTo(arcRectF, 90F, 90F)
        } else {
            arcRectF.set(
                itemLeft - bottomLeftCorner.radius,
                itemBottom - bottomLeftCorner.radius,
                itemLeft + bottomLeftCorner.radius,
                itemBottom + bottomLeftCorner.radius
            )
            path.arcTo(arcRectF, 0F, -90F)
        }


        path.lineTo(itemLeft, itemBottomWithDecoration)

        path.close()

        canvas.drawPath(path, paint)
    }
}

sealed class CardPosition {

    data class Whole(val corners: Array<RoundCorner>) : CardPosition() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Whole

            if (!corners.contentEquals(other.corners)) return false

            return true
        }

        override fun hashCode(): Int {
            return corners.contentHashCode()
        }
    }

    data class Top(val leftCorner: RoundCorner, val rightCorner: RoundCorner) : CardPosition()

    object Content : CardPosition()

    data class Bottom(val leftCorner: RoundCorner, val rightCorner: RoundCorner) : CardPosition()

}

data class RoundCorner(
    val radius: Float,
    val direction: Int = DIRECTION_OUT
) {

    companion object {
        const val DIRECTION_OUT: Int = 0
        const val DIRECTION_IN: Int = 1
    }
}

class RoundCornerBuilder {

    private val corners: Array<RoundCorner> = Array(4) { RoundCorner(0F) }

    fun topLeft(
        radius: Float,
        direction: Int = RoundCorner.DIRECTION_OUT
    ): RoundCornerBuilder {
        corners[0] = RoundCorner(radius, direction)
        return this
    }

    fun topRight(
        radius: Float,
        direction: Int = RoundCorner.DIRECTION_OUT
    ): RoundCornerBuilder {
        corners[1] = RoundCorner(radius, direction)
        return this
    }

    fun bottomLeft(
        radius: Float,
        direction: Int = RoundCorner.DIRECTION_OUT
    ): RoundCornerBuilder {
        corners[2] = RoundCorner(radius, direction)
        return this
    }

    fun bottomRight(
        radius: Float,
        direction: Int = RoundCorner.DIRECTION_OUT
    ): RoundCornerBuilder {
        corners[3] = RoundCorner(radius, direction)
        return this
    }

    fun build(): Array<RoundCorner> {
        return corners
    }
}