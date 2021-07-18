package com.hyh.list.internal

import com.hyh.list.ItemData

interface ItemSourceDelegate<Param : Any, Item : Any> {

    var sourcePosition: Int

    fun attach()

    fun getElementDiff(): IElementDiff<Item>

    fun mapItems(items: List<Item>): List<ItemData>

    fun onItemsDisplayed(items: List<Item>)

    fun onItemsChanged(changes: List<Triple<Item, Item, Any?>>)

    fun onItemsRecycled(items: List<Item>)

    fun detach()
}