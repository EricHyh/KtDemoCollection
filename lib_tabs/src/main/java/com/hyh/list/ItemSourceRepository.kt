package com.hyh.list

import com.hyh.list.internal.ItemSourceFetcher
import com.hyh.list.internal.RepoData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class ItemSourceRepository<Param : Any>(initialParam: Param?) {

    private val itemSourceFetcher = object : ItemSourceFetcher<Param>(initialParam) {

        override suspend fun getCache(param: Param, completeTimes: Int): CacheResult =
            this@ItemSourceRepository.getCache(param, completeTimes)

        override suspend fun load(param: Param): LoadResult =
            this@ItemSourceRepository.load(param)

        override fun getFetchDispatcher(param: Param): CoroutineDispatcher =
            this@ItemSourceRepository.getFetchDispatcher(param)

    }

    val flow: Flow<RepoData<Param>> = itemSourceFetcher.flow

    protected abstract suspend fun getCache(param: Param, completeTimes: Int): CacheResult

    protected abstract suspend fun load(param: Param): LoadResult

    protected open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    sealed class CacheResult {

        object Unused : CacheResult()

        class Success(
            val sources: List<ItemSourceInfo>,
        ) : CacheResult()
    }

    sealed class LoadResult {

        class Error(val error: Throwable) : LoadResult()

        class Success(
            val sources: List<ItemSourceInfo>,
        ) : LoadResult()
    }
}

data class ItemSourceInfo(
    val sourceToken: Any,
    val paramProvider: IParamProvider<out Any>,
    val lazySource: Lazy<IItemSource<out Any>>
)