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
    var itemData: ItemData
) {

    val attached: Boolean
        get() = itemData.delegate.attached
    val cached: Boolean
        get() = itemData.delegate.cached

    fun isSupportUpdateItemData(wrapper: ItemDataWrapper): Boolean {
        if (itemsBucketId != wrapper.itemsBucketId) return false
        if (itemsToken != wrapper.itemsToken) return false
        return itemData.isSupportUpdateItemData()
    }

    fun areItemsTheSame(wrapper: ItemDataWrapper): Boolean {
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


    override fun hashCode(): Int {
        var result = itemsBucketId
        result = 31 * result + itemsToken.hashCode()
        result = 31 * result + itemData.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemDataWrapper

        if (itemsBucketId != other.itemsBucketId) return false
        if (itemsToken != other.itemsToken) return false
        if (itemData != other.itemData) return false

        return true
    }
}