package com.hyh.tabs.adapter

import com.hyh.tabs.ITab
import com.hyh.tabs.internal.TabData

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/21
 */
interface ITabAdapter<Param : Any, Tab : ITab> {

    var currentPrimaryItem: Tab

    var tabCount: Int

    var tabTokens: List<Any>

    fun submitData(data: TabData<Param, Tab>)

    fun refresh(key: Param)

    fun retry()

}