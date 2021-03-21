package com.hyh.toast.core

import android.content.Context

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/3/11
 */
interface ToastFactory {

    fun init(context: Context)

    fun create(): IToast

}