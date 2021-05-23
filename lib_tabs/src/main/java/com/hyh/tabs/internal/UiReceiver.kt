package com.hyh.tabs.internal

import com.hyh.tabs.ITab
import com.hyh.tabs.TabInfo

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiver<Param : Any> {
    fun refresh(param: Param)
}

sealed class TabEvent<Tab : ITab>() {

    class Loading<Tab : ITab> : TabEvent<Tab>()

    class Error<Tab : ITab>(val error: Throwable) : TabEvent<Tab>()

    class Success<Tab : ITab>(val tabs: List<TabInfo<Tab>>) : TabEvent<Tab>()

}