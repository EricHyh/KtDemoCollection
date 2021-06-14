package com.hyh.list.internal

import com.hyh.list.IParamProvider
import com.hyh.list.ItemData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

data class RepoData<Param : Any>(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo<Param>
)

data class LazySourceData<Param : Any>(
    val sourceToken: Any,
    val paramProvider: IParamProvider<Param>,
    val lazyFlow: Deferred<Flow<SourceData<Param>>>
)

data class SourceData<Param : Any>(
    val flow: Flow<SourceEvent>,
    val receiver: UiReceiverForSource<Param>
)


sealed class RepoEvent {

    object Loading : RepoEvent()

    class UsingCache(val sources: List<LazySourceData<out Any>>) : RepoEvent()

    class Success(val sources: List<LazySourceData<out Any>>) : RepoEvent()

    class Error(val error: Throwable, val usingCache: Boolean) : RepoEvent()

}

sealed class SourceEvent {

    object Loading : SourceEvent()

    class PreShowing(val items: List<ItemData>) : SourceEvent()

    class Success(val items: List<ItemData>) : SourceEvent()

    class Error(val error: Throwable, val preShowing: Boolean) : SourceEvent()

}