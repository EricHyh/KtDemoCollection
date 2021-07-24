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
    fun destroy()
}

sealed class TabEvent<Tab : ITab>(val onReceived: (suspend () -> Unit)) {

    class Loading<Tab : ITab>(onReceived: (suspend () -> Unit) = {}) : TabEvent<Tab>(onReceived)

    class UsingCache<Tab : ITab>(
        val tabs: List<TabInfo<Tab>>,
        onReceived: (suspend () -> Unit) = {}
    ) : TabEvent<Tab>(onReceived)

    class Success<Tab : ITab>(
        val tabs: List<TabInfo<Tab>>,
        onReceived: (suspend () -> Unit) = {}
    ) : TabEvent<Tab>(onReceived)

    class Error<Tab : ITab>(
        val error: Throwable, val usingCache: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : TabEvent<Tab>(onReceived)

}