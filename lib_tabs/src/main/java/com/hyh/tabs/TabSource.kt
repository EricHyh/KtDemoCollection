package com.hyh.tabs

import com.hyh.base.RefreshStrategy
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
    initialParam: Param? = null
) {

    private val tabFetcher: TabFetcher<Param, Tab> = object : TabFetcher<Param, Tab>(initialParam) {
        override fun getRefreshStrategy(): RefreshStrategy = this@TabSource.getRefreshStrategy()
        override suspend fun getCache(params: CacheParams<Param, Tab>): CacheResult<Tab> = this@TabSource.getCache(params)
        override suspend fun load(params: LoadParams<Param, Tab>): LoadResult<Tab> = this@TabSource.load(params)
        override fun getFetchDispatcher(param: Param): CoroutineDispatcher = this@TabSource.getFetchDispatcher(param)
        override fun onDestroy() {
        }
    }

    val flow: Flow<TabData<Param, Tab>> = tabFetcher.flow

    protected open fun getRefreshStrategy(): RefreshStrategy {
        return RefreshStrategy.CancelLast
    }

    protected abstract suspend fun getCache(params: CacheParams<Param, Tab>): CacheResult<Tab>

    protected abstract suspend fun load(params: LoadParams<Param, Tab>): LoadResult<Tab>

    protected open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class CacheResult<Tab : ITab> {

        class Unused<Tab : ITab> : CacheResult<Tab>()

        data class Success<Tab : ITab> constructor(
            val tabs: List<TabInfo<Tab>>,
            val resultExtra: Any? = null
        ) : CacheResult<Tab>()
    }

    sealed class LoadResult<Tab : ITab> {

        data class Error<Tab : ITab>(
            val error: Throwable
        ) : TabSource.LoadResult<Tab>()

        data class Success<Tab : ITab> constructor(
            val tabs: List<TabInfo<Tab>>,
            val resultExtra: Any? = null
        ) : LoadResult<Tab>()
    }

    class CacheParams<Param : Any, Tab : ITab>(
        val param: Param,
        val displayedData: SourceDisplayedData<Tab>
    )

    class LoadParams<Param : Any, Tab : ITab>(
        val param: Param,
        val displayedData: SourceDisplayedData<Tab>
    )
}