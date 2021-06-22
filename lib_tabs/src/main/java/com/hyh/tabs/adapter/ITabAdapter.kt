package com.hyh.tabs.adapter


import com.hyh.tabs.ITab
import com.hyh.tabs.LoadState
import com.hyh.tabs.internal.TabData
import kotlinx.coroutines.flow.Flow

/**
 * TabAdapter 接口定义
 *
 * @author eriche
 * @data 2021/5/21
 */
interface ITabAdapter<Param : Any, Tab : ITab> {

    val currentPrimaryItem: Tab?

    val tabCount: Int

    val tabTokens: List<Any>?

    val tabTitles: List<CharSequence>?

    val loadStateFlow: Flow<LoadState>

    fun submitData(flow: Flow<TabData<Param, Tab>>)

    fun indexOf(tabToken: Any): Int {
        return tabTokens?.indexOf(tabToken) ?: -1
    }

    fun refresh(param: Param)

}