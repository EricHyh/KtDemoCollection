package com.hyh.paging3demo.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.XmlRes
import androidx.appcompat.widget.AppCompatTextView

class WeightText : AppCompatTextView {

    private val TAG = "WeightText"

    //region constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        //400 - 500 - 600 - 700
        /*val density = resources.displayMetrics.density
        val base = textSize / (30F * density)
        paint.strokeWidth = base * 2
        paint.style = Paint.Style.FILL_AND_STROKE*/
        paint.isFakeBoldText = true
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: $measuredWidth")
    }

}
