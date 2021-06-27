package com.hyh.list.internal

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
        val items: List<ItemData>,
        val listOperates: List<ListOperate>,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class Success(
        val items: List<ItemData>,
        val listOperates: List<ListOperate>,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class Error(
        val error: Throwable,
        val preShowing: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

}

enum class RefreshStage {
    UNBLOCK,
    TIMING,
    BLOCK
}

sealed class RefreshStrategy {

    object CancelLast : RefreshStrategy()
    object QueueUp : RefreshStrategy()
    data class DelayedQueueUp(val delay: Int) : RefreshStrategy()

}

sealed class BucketOperate {

    class OnAdded(bucketId: Int)

    class OnItemsTokenChanged(bucketId: Int, oldToken: Any, newToken: Any)

    class OnRemoved(bucketId: Int)

}