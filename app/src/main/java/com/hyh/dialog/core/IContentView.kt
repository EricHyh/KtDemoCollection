package com.hyh.dialog.core

import android.content.Context
import android.view.View
import android.view.ViewGroup

interface IContentView<T> {

    fun setup(windowInterface: WindowInterface, t: T?)

    fun onCreateView(context: Context, parent: ViewGroup?): View

    fun onDestroyView()

}