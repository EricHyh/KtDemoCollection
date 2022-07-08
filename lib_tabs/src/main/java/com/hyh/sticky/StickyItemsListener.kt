package com.hyh.sticky

import com.hyh.sticky.IStickyItemsAdapter
import com.hyh.sticky.StickyItemsLayout


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