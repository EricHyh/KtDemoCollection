package com.hyh.widget.sticky


/**
 * 吸顶、吸顶Item监听
 *
 * @author eriche 2022/7/5
 */
interface StickyItemsListener {

    fun onStickItemsAdded(
        parent: StickyItemsLayout,
        adapter: IStickyItemsAdapter<*>,
        position: Int
    )

    fun onStickItemsRemoved(
        parent: StickyItemsLayout,
        adapter: IStickyItemsAdapter<*>,
        position: Int
    )
}