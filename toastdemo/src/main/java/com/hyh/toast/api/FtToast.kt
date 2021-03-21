package com.hyh.toast.api

import android.content.Context
import com.hyh.toast.core.ApplicationTypeToastFactory

object FtToast {

    private lateinit var mContext: Context

    @JvmStatic
    fun init(context: Context) {
        mContext = context.applicationContext
        ApplicationTypeToastFactory.init(context)
    }

    /**
     * 常规Toast
     */
    @JvmStatic
    fun regularToast(): RegularToast {
        return RegularToast(mContext)
    }

    /**
     * 复合Toast
     */
    @JvmStatic
    fun multipleToast(): MultipleToast {
        return MultipleToast(mContext)
    }

    /**
     * 信息流Toast
     */
    @JvmStatic
    fun feedToast(): FeedToast {
        return FeedToast(mContext)
    }
}