package com.hyh.sticky

import androidx.recyclerview.widget.RecyclerView

/**
 * 列表粘性Item适配器
 *
 * @author eriche
 * @data 2020/11/30
 */
interface IStickyItemsAdapter<VH : RecyclerView.ViewHolder> {

    /**
     * 最大悬停头部布局数量
     */
    val maxStickyHeaders: Int

    /**
     * 最大悬停底部布局数量
     */
    val maxStickyFooters: Int

    /**
     * 最大固定悬停头部布局数量（不会被顶出去）
     */
    val maxFixedStickyHeaders: Int
        get() = 0

    /**
     * 最大固定悬停底部布局数量（不会被顶出去）
     */
    val maxFixedStickyFooters: Int
        get() = 0

    /**
     * 是否固定悬停头部布局（不会被顶出去）
     *
     * @param position
     * @return
     */
    fun isFixedStickyHeader(position: Int): Boolean = false

    /**
     * 是否悬停头部布局
     *
     * @param position
     * @return
     */
    fun isStickyHeader(position: Int): Boolean


    /**
     * 是否固定悬停底部布局（不会被顶出去）
     *
     * @param position
     * @return
     */
    fun isFixedStickyFooter(position: Int): Boolean = false

    /**
     * 是否悬停底部布局
     *
     * @param position
     * @return
     */
    fun isStickyFooter(position: Int): Boolean


    /**
     * 悬停布局绑定数据
     *
     * @param viewHolder 布局
     * @param position 数据索引
     */
    fun onBindStickyViewHolder(viewHolder: VH, position: Int)

}