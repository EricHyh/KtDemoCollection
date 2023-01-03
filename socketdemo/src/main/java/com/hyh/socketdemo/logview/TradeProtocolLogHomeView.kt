package com.hyh.socketdemo.logview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.hyh.socketdemo.R

/**
 * TODO: Add Description
 *
 * @author eriche 2022/12/29
 */
class TradeProtocolLogHomeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    companion object {
        private const val TAG = "TradeProtocolLogHomeView"
    }

    private val floatContainer: ConstraintLayout
    private val floatSettingBtn: View
    private val ivClear: View

    private val minFloatContainerWidth: Float
    private val minFloatContainerHeight: Float

    init {
        LayoutInflater.from(context).inflate(R.layout.debug_trade_protocol_log_view_home, this)
        floatContainer = findViewById(R.id.float_container)
        floatSettingBtn = findViewById(R.id.btn_float_setting)
        ivClear = findViewById(R.id.iv_clear)

        minFloatContainerWidth = resources.displayMetrics.density * 40
        minFloatContainerHeight = resources.displayMetrics.density * 80

        initFloatZoom()


    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatZoom() {
        var originalX = 0f
        var originalY = 0f
        findViewById<View>(R.id.iv_float_zoom).setOnTouchListener { v, event ->
            val currentX: Float = event.x
            val currentY: Float = event.y
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    originalX = currentX
                    originalY = currentY
                }
                MotionEvent.ACTION_MOVE -> {
                    updateFloatContainerSize(currentX - originalX, currentY - originalY)
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            true
        }
    }

    private fun updateFloatContainerSize(widthDiff: Float, heightDiff: Float) {
        val width = floatContainer.width
        val height = floatContainer.height

        val finalWidth: Int = (width + widthDiff)
            .coerceAtLeast(minFloatContainerWidth)
            .coerceAtMost(this.width.toFloat())
            .toInt()

        val finalHeight: Int = (height + heightDiff)
            .coerceAtLeast(minFloatContainerHeight)
            .coerceAtMost(this.height.toFloat())
            .toInt()

        val layoutParams = floatContainer.layoutParams
        layoutParams.width = finalWidth
        layoutParams.height = finalHeight
        floatContainer.requestLayout()
    }
}