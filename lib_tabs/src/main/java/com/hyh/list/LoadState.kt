package com.hyh.list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope

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


interface SourceLoadState


/**
 * [ItemSource]的加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class ItemSourceLoadState : SourceLoadState {

    object Initial : ItemSourceLoadState()

    object Loading : ItemSourceLoadState()

    data class PreShow(val itemCount: Int) : ItemSourceLoadState()

    data class Success(val itemCount: Int) : ItemSourceLoadState()

    data class Error(val error: Throwable, val preShowing: Boolean) : ItemSourceLoadState()

}


sealed class PagingSourceLoadState : SourceLoadState {

    object Initial : PagingSourceLoadState()

    object Refreshing : PagingSourceLoadState()

    data class RefreshSuccess(val endOfPaginationReached: Boolean) : PagingSourceLoadState()

    data class RefreshError(val error: Throwable) : PagingSourceLoadState()

    object Appending : PagingSourceLoadState()

    data class AppendError(val error: Throwable) : PagingSourceLoadState()

    data class AppendSuccess(val endOfPaginationReached: Boolean) : PagingSourceLoadState()
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
