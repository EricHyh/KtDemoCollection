package com.hyh.list.internal

import com.hyh.list.ItemData

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiverForRepo<Param: Any> {

    fun refresh(param: Param)

}

interface UiReceiverForSource<Param : Any> {

    fun refresh(param: Param)

}