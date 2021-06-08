package com.hyh.list.internal

import com.hyh.list.ItemData
import kotlinx.coroutines.flow.Flow

data class RepoData(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo
)

data class SourceData<Param : Any>(
    val sourceToken: Any,
    val lazyFlow: Lazy<Flow<SourceEvent>>,
    val lazyReceiver: Lazy<UiReceiverForSource<Param>>
)


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