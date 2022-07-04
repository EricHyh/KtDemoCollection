package com.hyh.list.internal

import com.hyh.list.FlatListItem

interface IItemOperator {

    fun moveItem(from: Int, to: Int): Boolean

    fun removeItem(position: Int, count: Int = 1)

    fun removeItem(item: FlatListItem)

    fun insertItems(position: Int, items: List<FlatListItem>)

}

class EmptyItemOperator : IItemOperator {

    override fun moveItem(from: Int, to: Int): Boolean = false

    override fun removeItem(position: Int, count: Int) {}

    override fun removeItem(item: FlatListItem) {}

    override fun insertItems(position: Int, items: List<FlatListItem>) {}

}