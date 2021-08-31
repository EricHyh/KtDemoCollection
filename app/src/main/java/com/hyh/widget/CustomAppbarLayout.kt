package com.hyh.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class CustomAppbarLayout(context: Context, attrs: AttributeSet) : AppBarLayout(context, attrs) {

    private val TAG = "CustomAppbarLayout"

    private var ignoreTouchEvent = false
    private var redispatchTouchEvent = false
    private var overTouchSlop = false
    private var initialX = 0f
    private var initialY = 0f
    private val touchSlop = 0f

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        Log.d(TAG, "onInterceptTouchEvent: $action")
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                overTouchSlop = false
                findBehavior()?.onTouchEvent(parent as CoordinatorLayout, this, ev) ?: super.onTouchEvent(ev)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val tx = ev.x - initialX
                val ty = ev.y - initialY
                return if (!overTouchSlop && (abs(tx) > touchSlop || abs(ty) > touchSlop)) {
                    overTouchSlop = true
                    abs(ty) > abs(tx)
                } else {
                    false
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                return false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "onTouchEvent: ${event.actionMasked}")
        return findBehavior()?.onTouchEvent(parent as CoordinatorLayout, this, event) ?: super.onTouchEvent(event)
    }

    private fun findBehavior(): CoordinatorLayout.Behavior<View>? {
        return (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
    }
}