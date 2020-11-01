package com.hyh.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class ItemHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {


    protected var data: T? = null
        private set

    private var positionFun: ((data: T) -> Int)? = null

    protected var itemPosition: Int = -1
        get() = data?.let { positionFun?.invoke(it) } ?: field

    fun isFullSpan(position: Int): Boolean {
        return false
    }

    internal fun onBindViewHolder(data: T, positionFun: (data: T) -> Int) {
        this.data = data
        this.positionFun = positionFun
        bindDataAndEvent()
    }

    protected abstract fun bindDataAndEvent()

    protected fun onRecycled() {}

    protected fun onViewAttachedToWindow() {}

    protected fun onViewDetachedFromWindow() {}

    protected fun onScrollStateChanged(newState: Int) {}

    protected fun onScrolled(scrollState: Int) {}

    protected fun onDestroy() {}
}