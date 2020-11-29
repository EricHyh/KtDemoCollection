package com.hyh.feeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class BaseFeedsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mItemDataList: List<IItemData>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType != 0) {
            val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            object : RecyclerView.ViewHolder(view) {}
        } else {
            object : RecyclerView.ViewHolder(View(parent.context)) {}
        }
    }

    override fun getItemCount(): Int {
        return mItemDataList?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < 0 || position >= itemCount) {
            return
        }
        mItemDataList?.get(position)?.onBindViewHolder(holder.itemView)
    }

    override fun getItemViewType(position: Int): Int {
        return mItemDataList?.get(position)?.getLayoutResId() ?: super.getItemViewType(position)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
    }

    fun setDataList() {

    }
}