package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.OnEventReceived
import com.hyh.list.FlatListItem
import kotlinx.coroutines.flow.Flow

data class RepoData<Param : Any>(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo<Param>
)

data class LazySourceData(
    val sourceToken: Any,
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

    class Error(val error: Throwable, val usingCache: Boolean, onReceived: OnEventReceived = {}) :
        RepoEvent(onReceived)

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
    var sources: List<BaseItemSource<out Any, out Any>>? = null,

    @Volatile
    var resultExtra: Any? = null
)


sealed class SourceEvent(val onReceived: (suspend () -> Unit)) {

    class Loading(onReceived: (suspend () -> Unit) = {}) : SourceEvent(onReceived)

    class PreShowing(
        val processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class RefreshSuccess(
        val processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class RefreshError(
        val error: Throwable,
        val preShowing: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    // region Paging

    class PagingRefreshSuccess(
        val processor: SourceResultProcessor,
        val noMore: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class PagingRefreshError(
        val error: Throwable,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class PagingAppendSuccess(
        val processor: SourceResultProcessor,
        val pageIndex: Int,
        val noMore: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class PagingAppendError(
        val error: Throwable,
        val pageIndex: Int,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    // endregion

    class ItemOperate(
        val processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)
}

typealias SourceResultProcessor = suspend () -> SourceProcessedResult

data class SourceProcessedResult(
    val resultItems: List<FlatListItem>,
    val listOperates: List<ListOperate>,
    val onResultUsed: Invoke
)

/**
 * 展示在界面上的列表数据
 *
 * @param Item
 * @property originalItems
 * @property flatListItems
 * @property resultExtra
 */
open class SourceDisplayedData<Item : Any>(

    /**
     * 原始数据
     */
    @Volatile
    var originalItems: List<Item>? = null,

    /**
     * 将原始数据转换成[FlatListItem]之后的数据
     */
    @Volatile
    var flatListItems: List<FlatListItem>? = null,

    /**
     * 额外数据，由业务自定义
     */
    @Volatile
    var resultExtra: Any? = null
)


class PagingSourceDisplayedData<Param : Any, Item : Any> : SourceDisplayedData<Item>() {

    @Volatile
    var pagingList: List<Paging<Param, Item>> = emptyList()

    val noMore
        get() = lastPaging?.noMore ?: false

    val appendParam: Param?
        get() = lastPaging?.nextParam

    val pagingSize: Int
        get() = pagingList.size

    val lastPaging: Paging<Param, Item>?
        get() = pagingList.lastOrNull()
}


class Paging<Param : Any, Item : Any> constructor(
    /**
     * 原始数据
     */
    @Volatile
    var originalItems: List<Item>? = null,

    /**
     * 将原始数据转换成[FlatListItem]之后的数据
     */
    @Volatile
    var flatListItems: List<FlatListItem>? = null,

    /**
     * 当前页参数
     */
    val param: Param? = null,

    /**
     * 下一页参数
     */
    val nextParam: Param?,

    /**
     * 是否没有更多
     */
    val noMore: Boolean

)