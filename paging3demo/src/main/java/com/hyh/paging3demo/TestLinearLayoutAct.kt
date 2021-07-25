package com.hyh.paging3demo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView

class TestLinearLayoutAct : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentView: ViewGroup = LayoutInflater.from(this)
            .inflate(R.layout.act_test_linear, null) as ViewGroup
        setContentView(contentView)

        val frameLayout = FrameLayout(this)
        val view = LayoutInflater.from(this).inflate(R.layout.test_linear, frameLayout)

        val linearLayout = view.findViewById<LinearLayout>(R.id.right_container)

        val childLinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.RED)
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }
        AppCompatTextView(this).apply {
            setTextColor(Color.BLACK)
            textSize = 0F
            isSingleLine = true
            //minHeight = 100
            ellipsize = TextUtils.TruncateAt.END
            text = "哈哈哈哈哈哈1"
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

            childLinearLayout.addView(
                this,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )

            setBackgroundColor(Color.BLUE)
        }
        AppCompatTextView(this).apply {
            setTextColor(Color.BLACK)
            textSize = 20F
            minHeight = 100
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            text = "哈哈哈哈哈哈2"
            gravity = Gravity.RIGHT or Gravity.TOP

            childLinearLayout.addView(
                this,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )

            //setBackgroundColor(Color.BLUE)
        }


        linearLayout.addView(childLinearLayout, ViewGroup.LayoutParams(300, ViewGroup.LayoutParams.MATCH_PARENT))

        contentView.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

}

class AutoMeasureLinearLayout : LinearLayout {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.makeMeasureSpec(
            0,
            View.MeasureSpec.UNSPECIFIED
        )
        super.onMeasure(width, heightMeasureSpec)
    }
}
