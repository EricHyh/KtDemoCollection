package com.hyh.tabs.adapter

import com.hyh.coroutine.SingleRunner
import com.hyh.tabs.ITab
import com.hyh.tabs.LoadState
import com.hyh.tabs.TabInfo
import com.hyh.tabs.internal.TabData
import com.hyh.tabs.internal.TabEvent
import com.hyh.tabs.internal.UiReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import java.util.*

/**
 * TabAdapter 基类
 *
 * @author eriche
 * @data 2021/5/20
 */
internal abstract class BaseTabAdapter<Param : Any, Tab : ITab>() : ITabAdapter<Param, Tab> {


    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()

    private var receiver: UiReceiver<Param>? = null
    private var tabs: List<TabInfo<Tab>>? = null

    override val tabTokens: List<Any>?
        get() = tabs?.map { it.tabToken }

    override val tabTitles: List<CharSequence>?
        get() = tabs?.map { it.tabTitle }

    private val _loadStateFlow: MutableStateFlow<LoadState> = MutableStateFlow(LoadState.Initial)

    override val loadStateFlow: Flow<LoadState>
        get() = _loadStateFlow

    override val tabCount: Int
        get() = tabs?.size ?: 0

    fun indexOf(tabInfo: TabInfo<Tab>): Int {
        return tabs?.indexOf(tabInfo) ?: -1
    }

    fun getTabInfo(position: Int): TabInfo<Tab>? {
        return tabs?.get(position)
    }

    fun getTabTitle(position: Int): CharSequence? {
        return tabs?.get(position)?.tabTitle
    }

    override suspend fun submitData(data: TabData<Param, Tab>) {
        collectFromRunner.runInIsolation {
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is TabEvent.UsingCache<Tab> -> {
                            val oldTabs = tabs
                            val newTabs = event.tabs
                            tabs = newTabs
                            if (!Arrays.equals(oldTabs?.toTypedArray(), newTabs.toTypedArray())) {
                                notifyDataSetChanged()
                            }
                            _loadStateFlow.value = LoadState.UsingCache(newTabs.size)
                        }
                        is TabEvent.Loading<Tab> -> {
                            _loadStateFlow.value = LoadState.Loading
                        }
                        is TabEvent.Error<Tab> -> {
                            _loadStateFlow.value = LoadState.Error(event.error, event.usingCache)
                        }
                        is TabEvent.Success<Tab> -> {
                            val oldTabs = tabs
                            val newTabs = event.tabs
                            tabs = newTabs
                            if (!Arrays.equals(oldTabs?.toTypedArray(), newTabs.toTypedArray())) {
                                notifyDataSetChanged()
                            }
                            _loadStateFlow.value = LoadState.Success(newTabs.size)
                        }
                    }
                }
            }
        }
    }

    override fun refresh(param: Param) {
        receiver?.refresh(param)
    }
}

