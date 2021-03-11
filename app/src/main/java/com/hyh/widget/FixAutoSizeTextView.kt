package com.hyh.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.hyh.demo.BuildConfig

/**
 * 用于解决RecyclerView中autoSize的TextView字体变小问题。
 * 问题：TextView使用autoSize属性(autoSizeMinTextSize..)，当recyclerView复用ViewHolder会导致TextView的字体越来越小。
 * 原因：复用的ViewHolder中TextView会按上一次字体大小显示，即使重新设置字体也无法生效。多次复用会导致字体越来越小。
 * 解决方法：先关闭textView缩小能力，然后设置内容以及字体大小，重新打开缩小能力。
 * 注：使用FixAutoSizeTextView务必添加：android:ellipsize="end",否则内容过长会截断显示
 * @author chauncywang
 * @date 2021/1/23
 */
class FixAutoSizeTextView : AppCompatTextView {
    //region constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    override fun setText(text: CharSequence, type: BufferType) {
        var minTextSize = 0
        var maxTextSize = 0
        var granularity = 0
        var doHack = TextViewCompat.getAutoSizeTextType(this) != TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
        if (doHack) {
            minTextSize = TextViewCompat.getAutoSizeMinTextSize(this);
            maxTextSize = TextViewCompat.getAutoSizeMaxTextSize(this);
            if (minTextSize <= 0 || maxTextSize <= minTextSize) {
                if (BuildConfig.DEBUG)
                    throw  AssertionError("fix FixAutoSizeTextView layout")
                doHack = false
            } else {
                granularity = TextViewCompat.getAutoSizeStepGranularity(this);
                if (granularity < 0)
                    granularity = 1
                TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, maxTextSize.toFloat());
                measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY));
                right = left
                bottom = top
                requestLayout()
            }
        }
        super.setText(text, type)
        if (doHack)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, minTextSize, maxTextSize, granularity, TypedValue.COMPLEX_UNIT_PX);
    }
}