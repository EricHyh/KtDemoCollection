package com.hyh.tabs.internal

import com.hyh.tabs.ITab
import com.hyh.tabs.TabInfo

/**
 * UI层发送给数据层的事件通道
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiver<Param : Any> {
    fun refresh(param: Param)
    fun close()
}

sealed class TabEvent<Tab : ITab> {

    class Loading<Tab : ITab> : TabEvent<Tab>()

    class UsingCache<Tab : ITab>(val tabs: List<TabInfo<Tab>>) : TabEvent<Tab>()

    class Success<Tab : ITab>(val tabs: List<TabInfo<Tab>>) : TabEvent<Tab>()

    class Error<Tab : ITab>(val error: Throwable, val usingCache: Boolean) : TabEvent<Tab>()
}