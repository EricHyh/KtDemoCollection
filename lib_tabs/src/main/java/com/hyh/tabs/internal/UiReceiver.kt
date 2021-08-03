package com.hyh.tabs.internal

/**
 * UI层发送给数据层的事件通道
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiver<Param : Any> {
    fun refresh(param: Param)
    fun destroy()
}

