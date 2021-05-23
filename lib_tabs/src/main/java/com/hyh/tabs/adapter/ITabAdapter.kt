package com.hyh.tabs.adapter


import com.hyh.tabs.ITab
import com.hyh.tabs.LoadState
import com.hyh.tabs.internal.TabData
import kotlinx.coroutines.flow.Flow

/**
 * TODO: Add Description
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

    suspend fun submitData(data: TabData<Param, Tab>)

    fun refresh(param: Param)

    fun notifyDataSetChanged()

}