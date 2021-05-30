package com.hyh.list

/**
 * 加载状态
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


sealed class SourceLoadState {

    object Initial : SourceLoadState()

    object Loading : SourceLoadState()

    data class PreShow(val itemCount: Int) : SourceLoadState()

    data class Success(val itemCount: Int) : SourceLoadState()

    data class Error(val error: Throwable, val preShow: Boolean) : SourceLoadState()

}