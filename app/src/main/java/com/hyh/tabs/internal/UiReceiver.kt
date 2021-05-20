package com.hyh.tabs.internal

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiver<Key : Any> {
    fun refresh(key: Key)
    fun retry()
}