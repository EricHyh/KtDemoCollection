package com.hyh.tabs.adapter

import com.hyh.tabs.ITab
import com.hyh.tabs.ITabProvider
import com.hyh.tabs.internal.TabData
import com.hyh.tabs.internal.UiReceiver

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
class TabDataHandler<Key : Any, Value : ITab> {

    private var receiver: UiReceiver<Key>? = null

    val tabCount: Int
        get() = 0


    fun getTab(position: Int): Value {
        TODO()
    }

    fun getTabFactory(): ITabProvider<Value> {
        TODO()
    }

    fun submitData(data: TabData<Key, Value>) {

    }

    fun refresh(key: Key) {
        receiver?.refresh(key)
    }

    fun retry() {
        receiver?.retry()
    }

    fun snapshot(): TabSnapshotList<Key, Value> {
        TODO()
    }
}


class TabSnapshotList<Key : Any, Value : ITab>(

    val key: Key,

    @Suppress
    val tabs: List<Value>

) : AbstractList<Value?>() {

    override val size: Int get() = tabs.size

    override fun get(index: Int): Value? {
        return tabs[index]
    }
}