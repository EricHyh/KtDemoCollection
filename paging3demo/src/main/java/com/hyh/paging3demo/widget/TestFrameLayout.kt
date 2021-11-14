package com.hyh.paging3demo.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import java.util.ArrayList

class TestFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val TAG = "TestFrameLayout"

    private val mMatchParentChildren = ArrayList<View>(1)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /*Log.d(TAG, "onMeasure: ${getSuggestedMinimumWidth()}, ${suggestedMinimumHeight}")
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(
            TAG,
            "onMeasure: ${MeasureSpec.getSize(widthMeasureSpec)}, ${
                MeasureSpec.getSize(heightMeasureSpec)
            }"
        )
        Log.d(TAG, "onMeasure: ${measuredWidth}, ${measuredHeight}")*/

        var count = childCount

        val measureMatchParentChildren =
            MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                    MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        mMatchParentChildren.clear()

        var maxHeight = 0
        var maxWidth = 0
        var childState = 0

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val lp = child.layoutParams as LayoutParams
                maxWidth = Math.max(
                    maxWidth,
                    child.measuredWidth + lp.leftMargin + lp.rightMargin
                )
                maxHeight = Math.max(
                    maxHeight,
                    child.measuredHeight + lp.topMargin + lp.bottomMargin
                )
                childState = combineMeasuredStates(childState, child.measuredState)
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT ||
                        lp.height == LayoutParams.MATCH_PARENT
                    ) {
                        mMatchParentChildren.add(child)
                    }
                }
            }
        }

        // Account for padding too

        // Account for padding too
        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom

        // Check against our minimum height and width

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, suggestedMinimumHeight)
        maxWidth = Math.max(maxWidth, suggestedMinimumWidth)

        // Check against our foreground's minimum height and width

        // Check against our foreground's minimum height and width
        val drawable = foreground
        if (drawable != null) {
            maxHeight = Math.max(maxHeight, drawable.minimumHeight)
            maxWidth = Math.max(maxWidth, drawable.minimumWidth)
        }

        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                maxHeight, heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )

        count = mMatchParentChildren.size
        if (count > 1) {
            for (i in 0 until count) {
                val child: View = mMatchParentChildren.get(i)
                val lp = child.layoutParams as MarginLayoutParams
                val childWidthMeasureSpec: Int
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    val width = Math.max(
                        0, measuredWidth
                                - paddingLeft - paddingRight
                                - lp.leftMargin - lp.rightMargin
                    )
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        width, MeasureSpec.EXACTLY
                    )
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        paddingTop + paddingBottom +
                                lp.leftMargin + lp.rightMargin,
                        lp.width
                    )
                }
                val childHeightMeasureSpec: Int
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    val height = Math.max(
                        0, (measuredHeight
                                - paddingTop - paddingBottom
                                - lp.topMargin - lp.bottomMargin)
                    )
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        height, MeasureSpec.EXACTLY
                    )
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        (paddingTop + paddingBottom +
                                lp.topMargin + lp.bottomMargin),
                        lp.height
                    )
                }
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }
    }

}