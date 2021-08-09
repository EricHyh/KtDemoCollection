package com.hyh.list

import androidx.lifecycle.LiveData
import com.hyh.coroutine.SimpleStateFlow

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


/**
 * [ItemSource]的加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class SourceLoadState {

    object Initial : SourceLoadState()

    object Loading : SourceLoadState()

    data class PreShow(val itemCount: Int) : SourceLoadState()

    data class Success(val itemCount: Int) : SourceLoadState()

    data class Error(val error: Throwable, val preShowing: Boolean) : SourceLoadState()

}


interface LoadStates {

    val sourceStates: List<SourceLoadState>

    val sourceStateCount: SimpleStateFlow<SourceStateCount>

}

data class SourceStateCount(
    val totalCount: Int,
    val initialCount: Int,
    val loadingCount: Int,
    val preShowCount: Int,
    val successCount: Int,
    val errorCount: Int
)