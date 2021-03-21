package com.hyh.toast.core

import android.view.View
import androidx.annotation.StyleRes

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/3/11
 */
interface IToast {

    fun setContentView(view: View)

    fun setFocusable(focusable: Boolean)

    fun setClickable(clickable: Boolean)

    fun setSize(width: Int, height: Int)

    fun setGravity(gravity: Int)

    fun setOffset(offsetX: Int, offsetY: Int)

    fun setAnimationStyle(@StyleRes animationStyle: Int)

    fun setOnShowListener(listener: () -> Unit)

    fun setOnDismissListener(listener: () -> Unit)

    fun show(): Boolean

    fun dismiss()

}