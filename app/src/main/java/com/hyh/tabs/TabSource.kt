package com.hyh.tabs

import com.hyh.tabs.internal.TabData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/**
 * Tab源
 *
 * @author eriche
 * @data 2021/5/20
 */
abstract class TabSource<Param : Any, Tab : ITab> {

    companion object {
        private const val TAG = "TabSource"
    }

    val flow: Flow<TabData<Param, Tab>> = flow {

    }

    abstract suspend fun load(params: Param): LoadResult<Tab>

    sealed class LoadResult<Tab : ITab> {

        data class Error<Tab : ITab>(
            val throwable: Throwable
        ) : TabSource.LoadResult<Tab>()

        data class TabResult<Tab : ITab> constructor(
            val tabChanged: Boolean,
            val tabProvider: ITabProvider<Tab>
        ) : LoadResult<Tab>()
    }
}