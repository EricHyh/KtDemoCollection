package com.hyh.list.internal

import com.hyh.list.ItemData

/**
 * [com.hyh.list.IItemData] 包装类
 *
 * @author eriche
 * @data 2021/6/24
 */
class ItemDataWrapper(
    val itemsBucketId: Int,
    val itemsToken: Any,
    val itemData: ItemData
) {


    fun isSupportUpdateItemData(): Boolean {
        return itemData.isSupportUpdateItemData()
    }

    fun areItemsTheSame(wrapper: ItemDataWrapper): Boolean {
        if (itemsBucketId != wrapper.itemsBucketId) return false
        if (itemsToken != wrapper.itemsToken) return false
        return itemData.areItemsTheSame(wrapper.itemData)
    }

    /**
     * 判断内容是否改变
     */
    fun areContentsTheSame(wrapper: ItemDataWrapper): Boolean = itemData.areContentsTheSame(wrapper.itemData)

    /**
     * 获取数据变动部分
     */
    fun getChangePayload(wrapper: ItemDataWrapper): Any? = itemData.getChangePayload(wrapper.itemData)

}