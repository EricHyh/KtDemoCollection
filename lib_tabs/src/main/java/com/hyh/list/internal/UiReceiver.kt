package com.hyh.list.internal

import androidx.lifecycle.Lifecycle
import com.hyh.list.FlatListItem

/**
 * UI层传递事件给数据层的通道
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiverForRepo<Param : Any> {

    fun injectParentLifecycle(lifecycle: Lifecycle)

    fun refresh(param: Param)

    fun destroy()

}

interface UiReceiverForSource {

    fun refresh(important: Boolean)

    fun accessItem(position: Int) {}

    fun removeItem(item: FlatListItem)

    fun removeItem(position: Int)

    fun move(from: Int, to: Int)

    fun destroy()
}