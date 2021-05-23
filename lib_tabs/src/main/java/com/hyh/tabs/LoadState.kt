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

    class Success(val tabCount: Int) : LoadState()

    class Error(val error: Throwable) : LoadState()

}