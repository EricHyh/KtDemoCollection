package com.hyh.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


abstract class IItemData<VH : RecyclerView.ViewHolder> {

    var _localPosition = -1

    val localPosition
        get() = _localPosition


    open fun isSupportUpdateItemData() = false
    open fun updateItemData(newItemData: ItemData) {}

    abstract fun getItemViewType(): Int

    abstract fun getViewHolderFactory(): ViewHolderFactory

    abstract fun onBindViewHolder(viewHolder: VH)

    open fun onBindViewHolder(viewHolder: VH, payloads: MutableList<Any>) = onBindViewHolder(viewHolder)

    /**
     * 判断是否为同一条数据.
     *
     * 例如，使用数据的唯一id作为判断是否为同一条数据的依据.
     */
    abstract fun areItemsTheSame(newItemData: ItemData): Boolean

    /**
     * 判断内容是否改变
     */
    abstract fun areContentsTheSame(newItemData: ItemData): Boolean

    /**
     * 获取数据变动部分
     */
    open fun getChangePayload(newItemData: ItemData): Any? = null

}

typealias ItemData = IItemData<out RecyclerView.ViewHolder>
typealias ViewHolderFactory = (parent: ViewGroup) -> RecyclerView.ViewHolder