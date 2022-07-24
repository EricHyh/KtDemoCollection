package com.hyh.list.internal

import androidx.lifecycle.Lifecycle
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.list.FlatListItem
import com.hyh.list.ItemSourceLoadState
import com.hyh.list.PagingSourceLoadState

/**
 * UI层传递事件给数据层的通道
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiverForRepo<Param> {

    fun attach(lifecycle: Lifecycle)

    fun refresh(param: Param)

    fun detach()

}

interface UiReceiverForSource : IItemOperator {

    fun refresh(important: Boolean)

    fun append(important: Boolean)

    fun rearrange(important: Boolean)

    fun accessItem(position: Int) {}

    fun destroy()
}