package com.hyh.list.internal

import com.hyh.OnEventReceived
import com.hyh.list.IParamProvider
import com.hyh.list.ItemData
import com.hyh.list.ItemSource
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

data class RepoData<Param : Any>(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo<Param>
)

data class LazySourceData<Param : Any>(
    val sourceToken: Any,
    val itemSource: ItemSource<Param>,
    val paramProvider: IParamProvider<Param>,
    val lazyFlow: Deferred<Flow<SourceData<Param>>>,
    val onReuse: (oldItemSource: ItemSource<Param>) -> Unit
)

data class SourceData<Param : Any>(
    val flow: Flow<SourceEvent>,
    val receiver: UiReceiverForSource<Param>
)


sealed class RepoEvent(val onReceived: OnEventReceived) {

    class Loading(onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class UsingCache(val sources: List<LazySourceData<out Any>>, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class Success(val sources: List<LazySourceData<out Any>>, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class Error(val error: Throwable, val usingCache: Boolean, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

}

sealed class SourceEvent(val onReceived: OnEventReceived) {

    class Loading(onReceived: OnEventReceived = {}) : SourceEvent(onReceived)

    class PreShowing(val items: List<ItemData>, onReceived: OnEventReceived = {}) : SourceEvent(onReceived)

    class Success(val items: List<ItemData>, onReceived: OnEventReceived = {}) : SourceEvent(onReceived)

    class Error(val error: Throwable, val preShowing: Boolean, onReceived: OnEventReceived = {}) : SourceEvent(onReceived)

}