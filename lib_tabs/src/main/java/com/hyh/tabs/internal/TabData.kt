package com.hyh.tabs.internal

import com.hyh.tabs.ITab
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