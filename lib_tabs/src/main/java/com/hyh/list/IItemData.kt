package com.hyh.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


interface IItemData<VH : RecyclerView.ViewHolder> {

    fun getItemType(): Int

    fun onCreateViewHolder(parent: ViewGroup): VH

    fun onBindViewHolder(viewHolder: VH)

}

typealias ItemData = IItemData<out RecyclerView.ViewHolder>