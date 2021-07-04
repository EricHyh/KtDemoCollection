package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.OnEventReceived
import com.hyh.list.ItemData
import com.hyh.list.ItemSource
import kotlinx.coroutines.flow.Flow

data class RepoData<Param : Any>(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo<Param>
)

data class LazySourceData(
    val itemSource: ItemSource<out Any, out Any>,
    val lazyFlow: Lazy<Flow<SourceData>>
)

data class SourceData(
    val flow: Flow<SourceEvent>,
    val receiver: UiReceiverForSource
)

sealed class RepoEvent(val onReceived: OnEventReceived) {

    class Loading(onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class UsingCache(
        val processor: RepoResultProcessor,
        onReceived: OnEventReceived = {}
    ) : RepoEvent(onReceived)

    class Success(
        val processor: RepoResultProcessor,
        onReceived: OnEventReceived = {}
    ) : RepoEvent(onReceived)

    class Error(val error: Throwable, val usingCache: Boolean, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

}

typealias RepoResultProcessor = suspend () -> RepoProcessedResult

data class RepoProcessedResult(
    val resultSources: List<LazySourceData>,
    val listOperates: List<ListOperate>,
    val onResultUsed: Invoke
)

class RepoDisplayedData(
    @Volatile
    var lazySources: List<LazySourceData>? = null,
    @Volatile
    var resultExtra: Any? = null
)


sealed class SourceEvent(val onReceived: (suspend () -> Unit)) {

    class Loading(onReceived: (suspend () -> Unit) = {}) : SourceEvent(onReceived)

    class PreShowing(
        val processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class Success(
        val processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class Error(
        val error: Throwable,
        val preShowing: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)
}

typealias SourceResultProcessor = suspend () -> SourceProcessedResult

data class SourceProcessedResult(
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