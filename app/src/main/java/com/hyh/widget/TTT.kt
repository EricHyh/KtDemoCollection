package com.hyh.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hyh.demo.R

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/11/24
 */
class TTT : FrameLayout {

    public constructor(context: Context) : super(context) {
        initView()
    }

    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }


    var childAt: View? = null

    private fun initView() {
        val inflate = LayoutInflater.from(context).inflate(R.layout.layout_ttt, this)
        Log.d("TAG", "initView:  ")
        childAt = getChildAt(0)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount == 1) {
            val childAt = getChildAt(0)
            val layoutParams = childAt.layoutParams
            if (layoutParams is LayoutParams) {
                layoutParams.gravity = Gravity.CENTER_VERTICAL
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        if (mode == MeasureSpec.EXACTLY) {
            val layoutParams = childAt?.layoutParams
            layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}