package com.hyh.toast.api

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes

class RegularToast(context: Context) : AbsToast<RegularToast>(context), AbsToast.DurationConfigurable<RegularToast> {

    companion object {
        private const val MAX_DEFAULT_WIDTH: Int = -1
    }

    override var mDuration: Int = 1000

    private var mIconRes: Int = 0

    private var mWidthFixed: Boolean = false

    private var mMaxWidth: Int = MAX_DEFAULT_WIDTH

    fun icon(@DrawableRes iconRes: Int): RegularToast {
        this.mIconRes = iconRes
        return this
    }

    fun widthFixed(widthFixed: Boolean): RegularToast {
        this.mWidthFixed = widthFixed
        return this;
    }

    fun maxWidth(maxWidth: Int): RegularToast {
        this.mMaxWidth = maxWidth
        return this;
    }

    override fun show() {
        val toast = mToastFactory.create()
        val textView = TextView(mContext).apply {
            text = mText
            setTextColor(Color.RED)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,)
        }
        toast.setContentView(textView)
        toast.show()
    }
}