package com.hyh.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.hyh.list.internal.ItemSourceFetcher
import com.hyh.base.RefreshStrategy
import com.hyh.list.internal.IFetcher
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.RepoData
import com.hyh.list.internal.RepoDisplayedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class ItemSourceRepo<Param>(initialParam: Param) : LifecycleOwner {

    private val itemSourceFetcher = object : ItemSourceFetcher<Param>(initialParam) {

        override fun getRefreshStrategy(): RefreshStrategy =
            this@ItemSourceRepo.getRefreshStrategy()

        override suspend fun getCache(params: CacheParams<Param>): CacheResult =
            this@ItemSourceRepo.getCache(params)

        override suspend fun load(params: LoadParams<Param>): LoadResult =
            this@ItemSourceRepo.load(params)

        override fun getFetchDispatcher(
            param: Param,
            displayedData: RepoDisplayedData
        ): CoroutineDispatcher =
            this@ItemSourceRepo.getFetchDispatcher(param, displayedData)

    }

    val flow: Flow<RepoData<Param>> = itemSourceFetcher.flow

    val fetcher: IFetcher<Param>
        get() = itemSourceFetcher

    final override fun getLifecycle(): Lifecycle {
        return itemSourceFetcher.lifecycleOwner.lifecycle
    }

    protected open fun getRefreshStrategy(): RefreshStrategy = RefreshStrategy.CancelLast

    protected abstract suspend fun getCache(params: CacheParams<Param>): CacheResult

    protected abstract suspend fun load(params: LoadParams<Param>): LoadResult

    protected open fun getFetchDispatcher(
        param: Param,
        displayedData: RepoDisplayedData
    ): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class CacheResult {

        object Unused : CacheResult()

        class Success(
            val sources: List<BaseItemSource<out Any, out Any>>,
            val resultExtra: Any? = null
        ) : CacheResult()
    }

    sealed class LoadResult {

        class Error(val error: Throwable) : LoadResult()

        class Success(
            val sources: List<BaseItemSource<*, *>>,
            val resultExtra: Any? = null

        ) : LoadResult()
    }

    class CacheParams<Param>(
        val param: Param,
        val displayedData: RepoDisplayedData
    )

    class LoadParams<Param>(
        val param: Param,
        val displayedData: RepoDisplayedData
    )
}