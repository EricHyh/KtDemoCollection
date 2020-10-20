package com.hyh.dialog.core

import android.view.View

interface IContentView<T> {

    fun setup(container: IContentContainer<T>, t: T?)

    fun onCreateView(): View

    fun onDestroyView()

}