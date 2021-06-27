package com.hyh.paging3demo.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

class TestView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val rectf = RectF()

    private val path = Path()

    private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 1F
    }

    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 1F
    }

    override fun onDraw(canvas: Canvas) {


        path.moveTo(0F, 0F)
        path.lineTo(0F, height.toFloat())

        rectf.set(0F, height - 20F, 40F, height + 20F)
        path.arcTo(rectf, 180F, 90F)

        path.lineTo(width - 20F, height - 20F)

        rectf.set(width - 40F, height - 20F, width.toFloat(), height + 20F)
        path.arcTo(rectf, 270F, 90F)

        path.lineTo(width.toFloat(), 0F)
        path.close()

        canvas.drawPath(path, paint2)

        //rectf.set(0F,0F,100F,100F)

        //path.arcTo(rectf, -180F, -90F)
        //canvas.drawArc(rectf, 180F, 90F, true, paint2)


        //canvas.drawRect(0F, height - 60F, 120F, height + 60F, paint1)
    }

}