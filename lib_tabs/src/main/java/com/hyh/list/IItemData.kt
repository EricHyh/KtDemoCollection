package com.hyh.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


interface IItemData<VH : RecyclerView.ViewHolder> {

    fun getItemViewType(): Int

    fun getViewHolderFactory(): ViewHolderFactory

    fun onBindViewHolder(viewHolder: VH)

    fun onBindViewHolder(viewHolder: VH, payloads: MutableList<Any>) = onBindViewHolder(viewHolder)

    /**
     * 判断是否为同一条数据.
     */
    fun areItemsTheSame(newItemData: ItemData): Boolean

    /**
     * 判断内容是否改变
     */
    fun areContentsTheSame(newItemData: ItemData): Boolean

    /**
     * 获取数据变动部分
     */
    fun getChangePayload(newItemData: ItemData): Any? = null

}

typealias ItemData = IItemData<out RecyclerView.ViewHolder>
typealias ViewHolderFactory = (parent: ViewGroup) -> RecyclerView.ViewHolder