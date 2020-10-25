package com.hyh.dialog.core

import android.app.Activity
import android.view.Gravity
import android.view.View
import androidx.fragment.app.Fragment

/**
 * TODO: Add Description
 *
 * @author eriche 2020/10/22
 */
class NNWindow<T> private constructor(builder: Builder<T>) {

    companion object {
        private const val TAG = "NNWindow"

        @JvmStatic
        fun <T> with(view: View): Builder<T> {
            return Builder()
        }

        @JvmStatic
        fun <T> with(fragment: Fragment): Builder<T> {
            return Builder()
        }

        @JvmStatic
        fun <T> with(activity: Activity): Builder<T> {
            return Builder()
        }
    }

    init {
        builder.t
    }

    fun show() {

    }

    fun dismiss() {

    }

    interface OnShowListener {
        fun onShow()
    }

    interface OnDismissListener {
        fun onDismiss()
    }

    class Builder<T> {

        var gravity: Int = Gravity.NO_GRAVITY

        var content: IContentView<T>? = null

        var t: T? = null

        var showListener: OnShowListener? = null

        var dismissListener: OnDismissListener? = null

        fun gravity(gravity: Int): Builder<T> {
            this.gravity = gravity
            return this
        }

        fun content(content: IContentView<T>): Builder<T> {
            this.content = content
            return this
        }

        fun eventListener(t: T): Builder<T> {
            this.t = t
            return this
        }

        fun showListener(listener: OnShowListener) {}

        fun dismissListener(listener: OnDismissListener) {}

        fun build(): NNWindow<T> {
            return NNWindow(this)
        }

        fun show() {

        }
    }
}
