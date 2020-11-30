package com.hyh.widget.sticky

import androidx.recyclerview.widget.RecyclerView

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/11/30
 */
interface IStickyHeadersAdapter<VH : RecyclerView.ViewHolder> {

    fun isStickyHeader(position: Int): Boolean

    fun onBindStickyViewHolder(viewHolder: VH, position: Int)

}