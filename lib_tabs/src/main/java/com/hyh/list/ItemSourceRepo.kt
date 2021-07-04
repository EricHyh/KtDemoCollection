package com.hyh.list

import com.hyh.list.internal.ItemSourceFetcher
import com.hyh.base.LoadStrategy
import com.hyh.list.internal.RepoData
import com.hyh.list.internal.RepoDisplayedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class ItemSourceRepo<Param : Any>(initialParam: Param?) {

    private val itemSourceFetcher = object : ItemSourceFetcher<Param>(initialParam) {

        override fun getLoadStrategy(): LoadStrategy =
            this@ItemSourceRepo.getLoadStrategy()

        override suspend fun getCache(params: CacheParams<Param>): CacheResult =
            this@ItemSourceRepo.getCache(params)

        override suspend fun load(params: LoadParams<Param>): LoadResult =
            this@ItemSourceRepo.load(params)

        override fun getFetchDispatcher(param: Param): CoroutineDispatcher =
            this@ItemSourceRepo.getFetchDispatcher(param)

    }

    val flow: Flow<RepoData<Param>> = itemSourceFetcher.flow

    protected open fun getLoadStrategy(): LoadStrategy = LoadStrategy.CancelLast

    protected abstract suspend fun getCache(params: CacheParams<Param>): CacheResult

    protected abstract suspend fun load(params: LoadParams<Param>): LoadResult

    protected open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class CacheResult {

        object Unused : CacheResult()

        class Success(
            val sources: List<ItemSource<out Any, out Any>>,
            val resultExtra: Any? = null
        ) : CacheResult()
    }

    sealed class LoadResult {

        class Error(val error: Throwable) : LoadResult()

        class Success(
            val sources: List<ItemSource<out Any, out Any>>,
            val resultExtra: Any? = null

        ) : LoadResult()
    }

    class CacheParams<Param : Any>(
        val param: Param,
        val displayedData: RepoDisplayedData
    )

    class LoadParams<Param : Any>(
        val param: Param,
        val displayedData: RepoDisplayedData
    )
}