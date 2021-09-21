package com.hyh.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.annotation.XmlRes
import androidx.appcompat.widget.AppCompatTextView

class CustomText : AppCompatTextView {

    private var mBrokerId: Int? = 1

    //region constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        refresh()
    }
    //endregion


    //region public

    fun setBrokerId(brokerId: Int?) {
        if (mBrokerId != brokerId) {
            this.mBrokerId = brokerId
            refresh()
        }
    }

    //endregion

    //region private
    private fun refresh() {
        mBrokerId.let { brokerId ->
            if (brokerId == null) {
                visibility = View.GONE
            } else {
                when (brokerId) {
                    1 -> {
                        visibility = View.VISIBLE
                        text = "11111"
                        setTextColor(Color.BLACK)
                        setBackgroundColor(Color.RED)
                    }
                    2 -> {
                        visibility = View.VISIBLE
                        text = "22222"
                        setTextColor(Color.BLACK)
                        setBackgroundColor(Color.BLUE)
                    }
                    3 -> {
                        text = "33333"
                        visibility = View.VISIBLE
                        setTextColor(Color.BLACK)
                        setBackgroundColor(Color.GRAY)
                    }
                    else -> visibility = View.GONE
                }
            }
        }
    }
    //endregion
}
