package com.hyh.widget.sticky

import androidx.recyclerview.widget.RecyclerView

/**
 * 列表粘性Item适配器
 *
 * @author eriche
 * @data 2020/11/30
 */
interface IStickyItemsAdapter<VH : RecyclerView.ViewHolder> {

    fun isFixedStickyHeader(position: Int): Boolean = false

    fun isStickyHeader(position: Int): Boolean

    fun isFixedStickyFooter(position: Int): Boolean = false

    fun isStickyFooter(position: Int): Boolean

    fun onBindStickyViewHolder(viewHolder: VH, position: Int)

}