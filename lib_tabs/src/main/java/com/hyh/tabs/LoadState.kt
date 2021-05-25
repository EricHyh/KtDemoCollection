package com.hyh.tabs

/**
 * Tab 加载状态
 *
 * @author eriche
 * @data 2021/1/29
 */
sealed class LoadState {

    object Initial : LoadState()

    object Loading : LoadState()

    data class UsingCache(val tabCount: Int) : LoadState()

    data class Success(val tabCount: Int) : LoadState()

    data class Error(val error: Throwable, val usingCache: Boolean) : LoadState()

}