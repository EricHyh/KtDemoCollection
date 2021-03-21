package com.hyh.toast.core

import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes

class ApplicationTypeToast(private val mWindowManager: WindowManager) : IToast {

    private var mContentView: View? = null

    private var mParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION
        height = WindowManager.LayoutParams.WRAP_CONTENT
        width = WindowManager.LayoutParams.WRAP_CONTENT
        //windowAnimations = android.R.style.Animation_Toast
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        gravity = Gravity.CENTER
    }

    private var mShowing = false

    override fun setContentView(view: View) {
        this.mContentView = view
    }

    override fun setFocusable(focusable: Boolean) {
        if (focusable) {
            mParams.flags = mParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            mParams.flags = mParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
    }

    override fun setClickable(clickable: Boolean) {
        if (clickable) {
            mParams.flags = mParams.flags and (
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                            and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv())
        } else {
            mParams.flags = mParams.flags or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
    }

    override fun setSize(width: Int, height: Int) {
        mParams.width = width
        mParams.height = height
    }

    override fun setGravity(gravity: Int) {
        mParams.gravity = gravity
    }

    override fun setOffset(offsetX: Int, offsetY: Int) {
        mParams.x = offsetX
        mParams.y = offsetY
    }

    override fun setAnimationStyle(@StyleRes animationStyle: Int) {
        mParams.windowAnimations = animationStyle
    }

    override fun setOnShowListener(listener: () -> Unit) {
    }

    override fun setOnDismissListener(listener: () -> Unit) {
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun show(): Boolean {
        if (mShowing) {
            return true
        }
        val contentView = mContentView ?: return false
        val result = mWindowManager.runCatching {
            mWindowManager.addView(contentView, mParams)
            mShowing = true
        }
        result.exceptionOrNull()?.printStackTrace()

        return result.isSuccess
    }

    override fun dismiss() {
        if (mShowing) {
            mContentView?.let {
                mWindowManager.runCatching {
                    mWindowManager.removeView(it)
                }
            }
        }
        mShowing = false
    }
}