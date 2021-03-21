package com.hyh.toast.api

import android.content.Context
import com.hyh.toast.core.ApplicationTypeToastFactory
import com.hyh.toast.core.ToastFactory

class FeedToast(context: Context) : AbsToast<FeedToast>(context) {

    protected override var mDuration: Int = 1200

    protected override var mToastFactory: ToastFactory = ApplicationTypeToastFactory

    private var mOffset: IntArray = IntArray(2) { 0 }

    fun offset(x: Int, y: Int): FeedToast {
        mOffset[0] = x
        mOffset[1] = y
        return this
    }

    override fun show() {

    }
}