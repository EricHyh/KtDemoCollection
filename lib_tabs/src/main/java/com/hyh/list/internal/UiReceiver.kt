package com.hyh.list.internal

/**
 * UI层传递事件给数据层的通道
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiverForRepo<Param : Any> {

    fun refresh(param: Param)

    fun close()

}

interface UiReceiverForSource {

    fun refresh(important: Boolean)

    fun close()
}