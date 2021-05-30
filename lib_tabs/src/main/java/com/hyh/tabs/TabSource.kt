package com.hyh.tabs

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
        override suspend fun getCache(param: Param, completeTimes: Int): CacheResult<Tab> = this@TabSource.getCache(param, completeTimes)
        override suspend fun load(param: Param): LoadResult<Tab> = this@TabSource.load(param)
        override fun getFetchDispatcher(param: Param): CoroutineDispatcher = this@TabSource.getFetchDispatcher(param)
    }

    val flow: Flow<TabData<Param, Tab>> = tabFetcher.flow

    protected open suspend fun getCache(param: Param, completeTimes: Int): CacheResult<Tab> = CacheResult.Unused()

    protected abstract suspend fun load(param: Param): LoadResult<Tab>

    protected open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class CacheResult<Tab : ITab> {

        class Unused<Tab : ITab> : CacheResult<Tab>()

        data class Success<Tab : ITab> constructor(
            val tabs: List<TabInfo<Tab>>
        ) : CacheResult<Tab>()
    }

    sealed class LoadResult<Tab : ITab> {

        data class Error<Tab : ITab>(
            val error: Throwable
        ) : TabSource.LoadResult<Tab>()

        data class Success<Tab : ITab> constructor(
            val tabs: List<TabInfo<Tab>>
        ) : LoadResult<Tab>()
    }
}