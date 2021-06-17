package com.hyh.list

/**
 * 实现了只在第一次获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleItemSource<Param : Any> : IItemSource<Param> {

    final override suspend fun getPreShow(params: IItemSource.PreShowParams<Param>): IItemSource.PreShowResult {
        return if (params.lastLoadResult == null && params.lastPreShowResult == null) {
            getPreShowWhenTheFirstTime(params.param)
        } else {
            IItemSource.PreShowResult.Unused
        }
    }

    final override suspend fun onPreShowResult(params: IItemSource.PreShowParams<Param>, preShowResult: IItemSource.PreShowResult) {}

    final override suspend fun load(params: IItemSource.LoadParams<Param>): IItemSource.LoadResult {
        return load(params.param)
    }

    final override suspend fun onLoadResult(params: IItemSource.LoadParams<Param>, loadResult: IItemSource.LoadResult) {}

    protected abstract suspend fun getPreShowWhenTheFirstTime(param: Param): IItemSource.PreShowResult

    protected abstract suspend fun load(param: Param): IItemSource.LoadResult

}