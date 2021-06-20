package com.hyh.list.internal

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiverForRepo<Param : Any> {

    fun refresh(param: Param)

}

interface UiReceiverForSource {

    fun refresh()

}