package com.hyh.toast.core

import android.view.View

class EmptyToast() : IToast {

    override fun setContentView(view: View) {
    }

    override fun setFocusable(focusable: Boolean) {
    }

    override fun setClickable(clickable: Boolean) {
    }

    override fun setSize(width: Int, height: Int) {
    }

    override fun setGravity(gravity: Int) {
    }

    override fun setOffset(offsetX: Int, offsetY: Int) {
    }

    override fun setAnimationStyle(animationStyle: Int) {
    }

    override fun setOnShowListener(listener: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun setOnDismissListener(listener: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun show(): Boolean {
        return false
    }

    override fun dismiss() {
    }
}