package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.OnEventReceived
import com.hyh.list.ItemData
import com.hyh.list.ItemSource
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

data class RepoData<Param : Any>(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo<Param>
)

data class LazySourceData(
    val sourceToken: Any,
    val itemSource: ItemSource<out Any, out Any>,
    val lazyFlow: Deferred<Flow<SourceData>>,
    val onReuse: (oldItemSource: ItemSource<out Any, out Any>) -> Unit
)

data class SourceData(
    val flow: Flow<SourceEvent>,
    val receiver: UiReceiverForSource
)


sealed class RepoEvent(val onReceived: OnEventReceived) {

    class Loading(onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class UsingCache(val sources: List<LazySourceData>, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class Success(val sources: List<LazySourceData>, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class Error(val error: Throwable, val usingCache: Boolean, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

}

sealed class SourceEvent(val onReceived: (suspend () -> Unit)) {

    class Loading(onReceived: (suspend () -> Unit) = {}) : SourceEvent(onReceived)

    class PreShowing(
        val processor: ResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class Success(
        val processor: ResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class Error(
        val error: Throwable,
        val preShowing: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

}

typealias ResultProcessor = suspend () -> ProcessedResult


data class ProcessedResult(
    val resultItems: List<ItemData>,
    val listOperates: List<ListOperate>,
    val onResultUsed: Invoke
)

class SourceDisplayedData<Item : Any>(
    @Volatile
    var items: List<Item>? = null,
    @Volatile
    var itemDataList: List<ItemData>? = null,
    @Volatile
    var resultExtra: Any? = null
)