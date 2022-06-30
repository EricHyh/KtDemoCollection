package com.hyh.list.internal

import com.hyh.list.FlatListItem

/**
 * [com.hyh.list.IFlatListItem] 包装类
 *
 * @author eriche
 * @data 2021/6/24
 */
class ListItemWrapper constructor(
    val itemsBucketId: Int,
    val itemsToken: Any,
    val item: FlatListItem
) {

    fun isSupportUpdateItemData(wrapperList: ListItemWrapper): Boolean {
        if (itemsBucketId != wrapperList.itemsBucketId) return false
        if (itemsToken != wrapperList.itemsToken) return false
        return item.isSupportUpdateItem()
    }

    fun areItemsTheSame(wrapperList: ListItemWrapper): Boolean {
        if (item.getItemViewType() != wrapperList.item.getItemViewType()) return false
        return item.areItemsTheSame(wrapperList.item)
    }

    /**
     * 判断内容是否改变
     */
    fun areContentsTheSame(wrapperList: ListItemWrapper): Boolean {
        if (itemsBucketId != wrapperList.itemsBucketId) return false
        if (itemsToken != wrapperList.itemsToken) return false
        return item.areContentsTheSame(wrapperList.item)
    }

    /**
     * 获取数据变动部分
     */
    fun getChangePayload(wrapperList: ListItemWrapper): Any? = item.getChangePayload(wrapperList.item)


    override fun hashCode(): Int {
        var result = itemsBucketId
        result = 31 * result + itemsToken.hashCode()
        result = 31 * result + item.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListItemWrapper

        if (itemsBucketId != other.itemsBucketId) return false
        if (itemsToken != other.itemsToken) return false
        if (item != other.item) return false

        return true
    }
}