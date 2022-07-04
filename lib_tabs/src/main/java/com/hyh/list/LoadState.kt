package com.hyh.list


/**
 * [ItemSourceRepo]的加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class RepoLoadState {

    object Initial : RepoLoadState()

    object Loading : RepoLoadState()

    data class UsingCache(val sourceCount: Int) : RepoLoadState()

    data class Success(val sourceCount: Int) : RepoLoadState()

    data class Error(val error: Throwable, val usingCache: Boolean) : RepoLoadState()

}


interface SourceLoadState {

    val currentItemCount: Int

}


/**
 * [ItemSource]的加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class ItemSourceLoadState(override val currentItemCount: Int) : SourceLoadState {

    object Initial : ItemSourceLoadState(0)

    data class Loading(override val currentItemCount: Int) : ItemSourceLoadState(currentItemCount)

    data class PreShow(override val currentItemCount: Int) : ItemSourceLoadState(currentItemCount)

    data class Success(override val currentItemCount: Int) : ItemSourceLoadState(currentItemCount)

    data class Error(override val currentItemCount: Int, val error: Throwable, val preShowing: Boolean) : ItemSourceLoadState(currentItemCount)

}


sealed class PagingSourceLoadState(override val currentItemCount: Int) : SourceLoadState {

    object Initial : PagingSourceLoadState(0)

    data class Refreshing(override val currentItemCount: Int) : PagingSourceLoadState(currentItemCount)

    data class RefreshSuccess(override val currentItemCount: Int, val endOfPaginationReached: Boolean) : PagingSourceLoadState(currentItemCount)

    data class RefreshError(override val currentItemCount: Int, val error: Throwable) : PagingSourceLoadState(currentItemCount)

    data class Appending(override val currentItemCount: Int) : PagingSourceLoadState(currentItemCount)

    data class AppendError(override val currentItemCount: Int, val error: Throwable) : PagingSourceLoadState(currentItemCount)

    data class AppendSuccess(override val currentItemCount: Int, val endOfPaginationReached: Boolean) : PagingSourceLoadState(currentItemCount)
}


class SourceLoadStates internal constructor(
    val sourceStateMap: Map<Any, ItemSourceLoadState>,
    val pagingSourceLoadState: PagingSourceLoadState = PagingSourceLoadState.Initial
) {

    companion object {
        internal val Initial = SourceLoadStates(emptyMap())
    }

    fun hasSuccessState(): Boolean {
        return sourceStateMap.values.find { it is ItemSourceLoadState.Success } != null
    }

    fun isPagingAppendComplete(): Boolean {
        return (pagingSourceLoadState is PagingSourceLoadState.RefreshSuccess
                && pagingSourceLoadState.endOfPaginationReached)
                ||
                (pagingSourceLoadState is PagingSourceLoadState.AppendSuccess
                        && pagingSourceLoadState.endOfPaginationReached)
    }
}

data class SourceStateCount(
    val totalCount: Int,
    val initialCount: Int,
    val loadingCount: Int,
    val preShowCount: Int,
    val successCount: Int,
    val errorCount: Int
)
