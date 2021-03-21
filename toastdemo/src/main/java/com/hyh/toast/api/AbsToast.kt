package com.hyh.toast.api

import android.content.Context
import android.view.View
import com.hyh.toast.core.ApplicationTypeToastFactory
import com.hyh.toast.core.ToastFactory

abstract class AbsToast<T : AbsToast<T>>(context: Context) {

    protected val mContext: Context = context

    protected open var mToastFactory: ToastFactory = ApplicationTypeToastFactory

    protected var mText: String? = null

    protected abstract var mDuration: Int

    protected var mAnchorView: View? = null

    fun text(text: String): T {
        mText = text
        return this as T
    }

    /**
     *
     */
    // TODO: 2021/3/20 讨论点，不知道 anchorView 的情况，例如接收到推送消息展示Toast
    fun anchorView(anchorView: View): T {
        mAnchorView = anchorView
        return this as T
    }

    abstract fun show()

    interface DurationConfigurable<T : AbsToast<T>> {

        fun duration(duration: Int): T {
            val toast: T = this as T
            toast.mDuration = duration
            return toast
        }
    }
}