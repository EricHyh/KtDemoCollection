package com.hyh.list.internal

import com.hyh.list.ItemData

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
interface UiReceiverForRepo {

    fun refresh()

}

interface UiReceiverForSource<Param : Any> {

    fun refresh(param: Param)

}

sealed class RepoEvent {

    object Loading : RepoEvent()

    class UsingCache(val sources: List<SourceData<out Any>>) : RepoEvent()

    class Success(val sources: List<SourceData<out Any>>) : RepoEvent()

    class Error(val error: Throwable, val usingCache: Boolean) : RepoEvent()

}

sealed class SourceEvent {

    object Loading : SourceEvent()

    class PreShowing(val items: List<ItemData>) : SourceEvent()

    class Success(val items: List<ItemData>) : SourceEvent()

    class Error(val error: Throwable, val preShowing: Boolean) : SourceEvent()

}
