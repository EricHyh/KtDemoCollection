package com.hyh.sticky

import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.IFlatListItem
import com.hyh.list.adapter.MultiItemSourceAdapter

/**
 * 基于[MultiItemSourceAdapter]实现的吸顶布局适配器
 *
 * @author eriche 2021/8/20
 */
open class MultiSourceStickyItemsAdapter(
    val adapter: MultiItemSourceAdapter<*>,
    override val maxStickyHeaders: Int,
    override val maxFixedStickyHeaders: Int = 0,
    override val maxStickyFooters: Int = 0,
    override val maxFixedStickyFooters: Int = 0
) : IStickyItemsAdapter<RecyclerView.ViewHolder> {

    override fun isStickyHeader(position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        return item is IStickyHeader
    }

    override fun isFixedStickyHeader(position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        return item is IStickyFixedHeader
    }

    override fun isStickyFooter(position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        return item is IStickyFooter
    }

    override fun isFixedStickyFooter(position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        return item is IStickyFixedFooter
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindStickyViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = adapter.getItem(position) ?: return
        onBindStickyViewHolder(viewHolder, (item as IFlatListItem<RecyclerView.ViewHolder>))
    }

    protected open fun onBindStickyViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        item: IFlatListItem<RecyclerView.ViewHolder>
    ) {
        item.bindViewHolder(viewHolder, emptyList())
    }
}

/**
 * 实现该接口，则表示这条 Item 需要吸顶展示
 */
interface IStickyHeader
interface IStickyFixedHeader : IStickyHeader    //固定吸顶

/**
 * 实现该接口，则表示这条 Item 需要吸底展示
 */
interface IStickyFooter
interface IStickyFixedFooter : IStickyFooter    //固定吸底