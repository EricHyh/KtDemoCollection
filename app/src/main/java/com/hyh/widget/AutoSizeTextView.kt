package com.hyh.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.hyh.demo.R
import kotlin.math.roundToInt

/**
 * 根据宽度自动计算文字大小的TextView，解决重新设置文字，不重新计算文字大小的问题.
 *
 * @author eriche
 * @data 2021/1/26
 */
@SuppressLint("CustomViewStyleable")
class AutoSizeTextView : AppCompatTextView {

    private var autoMaxTextSizeInPx = 1f
    private var autoMinTextSizeInPx = 1f
    private var autoTextSizeStepInPx = 1f
    private var maxWidthPercent = -1.0F

    private var mTextPaint: Paint? = null

    private var mLastText: CharSequence? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoSizeTextView, defStyle, 0)
        autoMaxTextSizeInPx = a.getDimension(R.styleable.AutoSizeTextView_autoMaxTextSize, 1f)
        autoMinTextSizeInPx = a.getDimension(R.styleable.AutoSizeTextView_autoMinTextSize, 1f)
        autoTextSizeStepInPx = a.getDimension(R.styleable.AutoSizeTextView_autoTextSizeStep, 1f)
        maxWidthPercent = a.getFloat(R.styleable.AutoSizeTextView_maxWidthPercent, 1.0F)
        a.recycle()
        getTextPaint().textSize = autoMaxTextSizeInPx

        setTextSizeInternal(autoMaxTextSizeInPx)
    }

    private fun init(context: Context, attrs: AttributeSet?) {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = getMaxWith()
        if (maxWidth <= 0 || maxWidthPercent <= 0.0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var useWidthMeasureSpec = widthMeasureSpec
        if (widthMode != MeasureSpec.EXACTLY) {
            val expectedMaxWidth = getExpectedMaxWidth()
            if (expectedMaxWidth > maxWidth) {
                useWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY)
            } else {
                useWidthMeasureSpec = MeasureSpec.makeMeasureSpec(expectedMaxWidth.roundToInt(), MeasureSpec.EXACTLY)
            }
        } else {
            if (widthSize > maxWidth) {
                useWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY)
            }
        }
        super.onMeasure(useWidthMeasureSpec, heightMeasureSpec)
    }

    fun setMaxWidthPercent(maxWidthPercent: Float) {
        if (this.maxWidthPercent != maxWidthPercent) {
            this.maxWidthPercent = maxWidthPercent
            post {
                requestLayout()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setTextSizeInternal(getSuitableTextSize(measuredWidth))
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (mLastText == null || mLastText?.equals(text) == false) {
            val maxWith = getMaxWith()
            if (maxWith > 0) {
                setTextSizeInternal(getSuitableTextSize(maxWith))
            } else if (autoMaxTextSizeInPx > 0) {
                setTextSizeInternal(autoMaxTextSizeInPx)
            } else if (measuredWidth > 0) {
                setTextSizeInternal(getSuitableTextSize(measuredWidth))
            }
        }
        mLastText = text
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d("AutoSizeTextView", "onDraw: textSize = $textSize, measuredWidth = $measuredWidth")
    }

    private fun getSuitableTextSize(width: Int): Float {
        val drawables = compoundDrawables
        //目前不考虑高度的距离，因此只需要算左、右图片的宽度
        var leftDrawableWidth = 0
        var rightDrawableWidth = 0
        if (drawables[0] != null) {
            leftDrawableWidth = drawables[0]?.bounds?.width() ?: 0
        }
        if (drawables[2] != null) {
            rightDrawableWidth = drawables[2]?.bounds?.width() ?: 0
        }
        val availableWidth = width - this.paddingLeft - this.paddingRight - this.compoundDrawablePadding - leftDrawableWidth - rightDrawableWidth
        var trySize: Float = autoMaxTextSizeInPx
        getTextPaint().textSize = trySize
        while (getTextWidth() > availableWidth) {
            trySize -= autoTextSizeStepInPx
            if (trySize <= autoMinTextSizeInPx) {
                trySize = autoMinTextSizeInPx
                break
            }
            getTextPaint().textSize = trySize
        }
        return trySize
    }

    private fun getExpectedMaxWidth(): Float {
        val drawables = compoundDrawables
        var leftDrawableWidth = 0
        var rightDrawableWidth = 0
        if (drawables[0] != null) {
            leftDrawableWidth = drawables[0]?.bounds?.width() ?: 0
        }
        if (drawables[2] != null) {
            rightDrawableWidth = drawables[2]?.bounds?.width() ?: 0
        }
        return getTextPaint().apply { textSize = autoMaxTextSizeInPx }.measureText(
            text?.toString()
                ?: ""
        ) + this.paddingLeft + this.paddingRight + this.compoundDrawablePadding + leftDrawableWidth + rightDrawableWidth
    }

    private fun getTextWidth(): Float {
        val text = text?.toString() ?: ""
        var width = 0.0f
        if (!TextUtils.isEmpty(text)) {
            val items = text.split("\n".toRegex()).toTypedArray()
            var itemWidth = 0.0f
            for (i in items.indices) {
                itemWidth = getTextPaint().measureText(items[i])
                if (itemWidth > width) {
                    width = itemWidth
                }
            }
        }
        return width
    }

    private fun setTextSizeInternal(size: Float) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        Log.d("AutoSizeTextView", "setTextSize: size = $size, textSize = $textSize, measuredWidth = $measuredWidth")
        post {
            requestLayout()
        }
    }

    private fun getTextPaint(): Paint {
        return mTextPaint.let { paint ->
            paint ?: Paint().apply {
                mTextPaint = this
            }
        }
    }

    private fun getMaxWith() = (getParentWith() * maxWidthPercent).roundToInt()

    private fun getParentWith(): Int {
        val parent = parent
        if (parent is View) {
            return parent.measuredWidth
        }
        return 0
    }
}