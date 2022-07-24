package com.hyh.list.internal

import com.hyh.Invoke
import com.hyh.OnEventReceived
import com.hyh.list.FlatListItem
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.utils.ListOperate
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

data class RepoData<Param>(
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
    var sources: List<BaseItemSource<*, *>>? = null,

    @Volatile
    var sourcesMap: Map<Any, BaseItemSource<*, *>>? = null,

    @Volatile
    var resultExtra: Any? = null
) {
    internal fun clear() {
        lazySources = null
        sources = null
        sourcesMap = null
        resultExtra = null
    }
}


sealed class SourceEvent(val onReceived: (suspend () -> Unit)) {

    abstract class ProcessorSourceEvent(
        val processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit)
    ) : SourceEvent(onReceived)

    class Loading(onReceived: (suspend () -> Unit) = {}) : SourceEvent(onReceived)

    class PreShowing(
        processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : ProcessorSourceEvent(processor, onReceived)

    class RefreshSuccess(
        processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : ProcessorSourceEvent(processor, onReceived)

    class RefreshError(
        val error: Throwable,
        val preShowing: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    // region Paging

    class PagingRefreshing(onReceived: (suspend () -> Unit) = {}) : SourceEvent(onReceived)

    class PagingRefreshSuccess(
        processor: SourceResultProcessor,
        val endOfPaginationReached: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : ProcessorSourceEvent(processor, onReceived)

    class PagingRefreshError(
        val error: Throwable,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class PagingAppending(onReceived: (suspend () -> Unit) = {}) : SourceEvent(onReceived)

    class PagingAppendSuccess(
        processor: SourceResultProcessor,
        val pageIndex: Int,
        val endOfPaginationReached: Boolean,
        onReceived: (suspend () -> Unit) = {}
    ) : ProcessorSourceEvent(processor, onReceived)

    class PagingAppendError(
        val error: Throwable,
        val pageIndex: Int,
        onReceived: (suspend () -> Unit) = {}
    ) : SourceEvent(onReceived)

    class PagingRearrangeSuccess(
        processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : ProcessorSourceEvent(processor, onReceived)

    // endregion

    class ItemOperate(
        processor: SourceResultProcessor,
        onReceived: (suspend () -> Unit) = {}
    ) : ProcessorSourceEvent(processor, onReceived)
}

typealias SourceResultProcessor = suspend () -> SourceProcessedResult

data class SourceProcessedResult constructor(
    val resultItems: List<FlatListItem>,
    val listOperates: List<ListOperate>,
    val onResultUsed: Invoke
)

/**
 * 展示在界面上的列表数据
 *
 * @property flatListItems
 * @property resultExtra
 */
open class SourceDisplayedData(

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


class PagingSourceDisplayedData<Param> : SourceDisplayedData() {

    @Volatile
    var pagingList: List<Paging<Param>> = emptyList()

    val noMore
        get() = lastPaging?.noMore ?: false

    val appendParam: Param?
        get() = lastPaging?.nextParam

    val pagingSize: Int
        get() = pagingList.size

    val lastPaging: Paging<Param>?
        get() = pagingList.lastOrNull()
}


class Paging<Param> constructor(

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