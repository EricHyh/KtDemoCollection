package com.hyh.tabs.internal

import com.hyh.Invoke
import com.hyh.tabs.ITab
import com.hyh.tabs.TabInfo
import kotlinx.coroutines.flow.Flow

/**
 * 数据层与Ui层桥梁
 *
 * @author eriche
 * @data 2021/5/20
 */
class TabData<Param : Any, Tab : ITab>(
    val flow: Flow<TabEvent<Tab>>,
    val receiver: UiReceiver<Param>
)

sealed class TabEvent<Tab : ITab>(val onReceived: (suspend () -> Unit)) {

    class Loading<Tab : ITab>(onReceived: (suspend () -> Unit) = {}) : TabEvent<Tab>(onReceived)

    class UsingCache<Tab : ITab>(
        val processor: TabSourceResultProcessor<Tab>,
        onReceived: (suspend () -> Unit) = {}
    ) : TabEvent<Tab>(onReceived)

    class Success<Tab : ITab>(
        val processor: TabSourceResultProcessor<Tab>,
        onReceived: (suspend () -> Unit) = {}
    ) : TabEvent<Tab>(onReceived)

    class Error<Tab : ITab>(
        val error: Throwable, val usingCache: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : TabEvent<Tab>(onReceived)

}

class TabSourceProcessedResult<Tab : ITab>(
    val tabs: List<TabInfo<Tab>>,
    val changed: Boolean,
    val onResultUsed: Invoke
)

typealias TabSourceResultProcessor<Tab> = () -> TabSourceProcessedResult<Tab>

class SourceDisplayedData<Tab : ITab>(

    @Volatile
    var tabs: List<TabInfo<Tab>>? = null,

    /**
     * 额外数据，由业务自定义
     */
    @Volatile
    var resultExtra: Any? = null

)