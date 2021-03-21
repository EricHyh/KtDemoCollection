package com.hyh.toast.core

import java.lang.ref.WeakReference

class KeepSingleToastHelper {

    private var mToastRef: WeakReference<IToast>? = null

    fun onToastShow(toast: IToast) {

    }

    fun onToastDismiss(toast: IToast) {

    }
}