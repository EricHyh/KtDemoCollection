package com.hyh.toast.api

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.hyh.toast.R

class MultipleToast(context: Context) : AbsToast<MultipleToast>(context), AbsToast.DurationConfigurable<MultipleToast> {

    protected override var mDuration: Int = 5000

    private var mIconRes: Int = 0

    private var mClickText: String? = null

    private var mClickAction: (() -> Unit)? = null

    fun icon(@DrawableRes iconRes: Int): MultipleToast {
        this.mIconRes = iconRes
        return this
    }

    fun clickText(text: String): MultipleToast {
        this.mClickText = text
        return this
    }

    fun clickAction(action: () -> Unit): MultipleToast {
        this.mClickAction = action
        return this
    }

    override fun show() {
        val toast = mToastFactory.create()
        toast.setClickable(true)
        toast.setAnimationStyle(R.style.toastAnim)
        val textView = TextView(mContext).apply {
            text = mText
            setTextColor(Color.WHITE)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.RED)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                mClickAction?.invoke()
                toast.dismiss()
            }
        }
        toast.setContentView(textView)
        toast.show()
    }
}