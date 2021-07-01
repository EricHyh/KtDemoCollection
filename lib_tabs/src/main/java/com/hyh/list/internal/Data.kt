package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.InvokeWithParam
import com.hyh.OnEventReceived
import com.hyh.SuspendInvoke
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
    val lazyFlow: Deferred<Flow<SourceData>>,
    val onReuse: (oldItemSource: ItemSource<Param>) -> Unit
)

data class SourceData(
    val flow: Flow<SourceEvent>,
    val receiver: UiReceiverForSource
)


sealed class RepoEvent(val onReceived: OnEventReceived) {

    class Loading(onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class UsingCache(val sources: List<LazySourceData<out Any>>, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

    class Success(val sources: List<LazySourceData<out Any>>, onReceived: OnEventReceived = {}) : RepoEvent(onReceived)

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

typealias ResultProcessor = suspend (
    displayedItemWrappers: List<ItemDataWrapper>?,
    displayedItemsBucketMap: Map<Int, ItemSource.ItemsBucket>?
) -> ProcessedResult


data class ProcessedResult(
    val resultItemWrappers: List<ItemDataWrapper>,
    val resultItems: List<ItemData>,
    val resultItemsBucketMap: Map<Int, ItemSource.ItemsBucket>,
    val listOperates: List<ListOperate>,
    val onResultUsed: Invoke
)

class SourceDisplayedData(
    var itemsBucketIds: List<Int>? = null,
    var itemsBucketMap: Map<Int, ItemSource.ItemsBucket>? = null,
    var itemWrappers: List<ItemDataWrapper>? = null,
    var items: List<ItemData>? = null,
)