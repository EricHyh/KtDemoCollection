package com.hyh.tabs.adapter

import com.hyh.coroutine.SimpleMutableStateFlow
import com.hyh.coroutine.SingleRunner
import com.hyh.page.PageContext
import com.hyh.tabs.ITab
import com.hyh.tabs.LoadState
import com.hyh.tabs.TabInfo
import com.hyh.tabs.internal.TabData
import com.hyh.tabs.internal.TabEvent
import com.hyh.tabs.internal.TabSourceResultProcessor
import com.hyh.tabs.internal.UiReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import java.util.*

/**
 * [ITabAdapter]基类
 *
 * @author eriche
 * @data 2021/5/20
 */
internal abstract class BaseTabAdapter<Param : Any, Tab : ITab>(private val pageContext: PageContext) : ITabAdapter<Param, Tab> {

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()

    private var receiver: UiReceiver<Param>? = null
    private var tabs: List<TabInfo<Tab>>? = null

    override val tabTokens: List<Any>?
        get() = tabs?.map { it.tabToken }

    override val tabTitles: List<CharSequence>?
        get() = tabs?.map { it.tabTitle }

    private val _loadStateFlow: SimpleMutableStateFlow<LoadState> = SimpleMutableStateFlow(LoadState.Initial)

    override val loadStateFlow: Flow<LoadState>
        get() = _loadStateFlow.asStateFlow()

    override val tabCount: Int
        get() = tabs?.size ?: 0


    init {
        pageContext.invokeOnDestroy {
            performDestroy()
        }
    }

    internal fun indexOf(tabInfo: TabInfo<Tab>): Int {
        return tabs?.indexOf(tabInfo) ?: -1
    }

    internal fun getTabInfo(position: Int): TabInfo<Tab>? {
        return tabs?.get(position)
    }

    internal fun getTabTitle(position: Int): CharSequence? {
        return tabs?.get(position)?.tabTitle
    }

    override fun submitData(flow: Flow<TabData<Param, Tab>>) {
        pageContext.lifecycleScope.launch {
            flow.collectLatest {
                submitData(it)
            }
        }
    }

    override fun refresh(param: Param) {
        receiver?.refresh(param)
    }

    protected abstract fun notifyDataSetChanged()

    private suspend fun submitData(data: TabData<Param, Tab>) {
        collectFromRunner.runInIsolation {
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is TabEvent.UsingCache<Tab> -> {
                            val newTabs = updateTabs(event.processor)
                            _loadStateFlow.value = LoadState.UsingCache(newTabs.size)
                        }
                        is TabEvent.Loading<Tab> -> {
                            _loadStateFlow.value = LoadState.Loading
                        }
                        is TabEvent.Error<Tab> -> {
                            _loadStateFlow.value = LoadState.Error(event.error, event.usingCache)
                        }
                        is TabEvent.Success<Tab> -> {
                            val newTabs = updateTabs(event.processor)
                            _loadStateFlow.value = LoadState.Success(newTabs.size)
                        }
                    }
                    event.onReceived()
                }
            }
        }
    }

    private fun updateTabs(processor: TabSourceResultProcessor<Tab>): List<TabInfo<Tab>> {
        val result = processor.invoke()
        val newTabs = result.tabs
        this.tabs = newTabs
        result.onResultUsed()
        if (result.changed) {
            notifyDataSetChanged()
        }
        return newTabs
    }

    private fun performDestroy() {
        receiver?.destroy()
    }
}