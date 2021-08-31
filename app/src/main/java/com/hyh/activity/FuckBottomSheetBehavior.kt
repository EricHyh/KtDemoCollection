package com.hyh.activity

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs

class FuckBottomSheetBehavior<V : View>(context: Context) : BottomSheetBehavior<V>(context, null) {

    private var initialY: Float = 0F

    /*override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        var scrollUp: Boolean? = null
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val ty = event.y - initialY
                if (abs(ty) > ViewConfiguration.get(parent.context).scaledTouchSlop) {
                    //scrollUp = (ty < 0)
                }
            }
        }
        val onInterceptTouchEvent = super.onInterceptTouchEvent(parent, child, event)
        if (onInterceptTouchEvent && scrollUp != null) {
            return !scrollUp
        }
        return onInterceptTouchEvent
    }*/
}