package com.hyh.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class ItemHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    protected var list: List<T>? = null
        private set

    var data: T? = null
        private set

    protected val itemPosition: Int
        get() = if (list == null) -1 else list!!.indexOf(data)

    protected fun isFullSpan(position: Int): Boolean {
        return false
    }

    internal fun onBindViewHolder(list: List<T>, position: Int) {
        this.list = list
        if (this.list != null && position < this.list!!.size) {
            this.data = list[position]
        }
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