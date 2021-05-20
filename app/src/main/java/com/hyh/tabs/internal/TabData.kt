package com.hyh.tabs.internal

import com.hyh.tabs.ITab
import com.hyh.tabs.TabSource
import kotlinx.coroutines.flow.Flow

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
class TabData<Key : Any, Value : ITab>(
    val flow: Flow<TabSource.LoadResult<Value>>,
    val receiver: UiReceiver<Key>
)