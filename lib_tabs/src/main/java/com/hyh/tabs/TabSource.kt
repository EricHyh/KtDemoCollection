package com.hyh.tabs

import com.hyh.page.PageContext
import com.hyh.tabs.internal.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Tab源
 *
 * @author eriche
 * @data 2021/5/20
 */
abstract class TabSource<Param : Any, Tab : ITab>(
    initialParam: Param
) {

    private val tabFetcher: TabFetcher<Param, Tab> = object : TabFetcher<Param, Tab>(initialParam) {
        override suspend fun load(param: Param): LoadResult<Tab> = this@TabSource.load(param)
        override fun getFetchDispatcher(param: Param): CoroutineDispatcher = this@TabSource.getFetchDispatcher(param)
    }

    val flow: Flow<TabData<Param, Tab>> = tabFetcher.flow

    protected abstract suspend fun load(param: Param): LoadResult<Tab>

    protected fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class LoadResult<Tab : ITab> {

        data class Error<Tab : ITab>(
            val error: Throwable
        ) : TabSource.LoadResult<Tab>()

        data class Success<Tab : ITab> constructor(
            val tabs: List<TabInfo<Tab>>
        ) : LoadResult<Tab>()
    }
}