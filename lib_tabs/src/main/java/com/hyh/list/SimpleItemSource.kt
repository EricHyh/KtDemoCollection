package com.hyh.list

/**
 * 实现了只在第一次获取缓存的逻辑
 *
 * @author eriche
 * @data 2021/6/17
 */
abstract class SimpleItemSource<Param : Any> : ItemSource<Param>() {

    final override suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult {
        return if (params.lastLoadResult == null && params.lastPreShowResult == null) {
            getPreShowWhenTheFirstTime(params.param)
        } else {
            PreShowResult.Unused
        }
    }

    final override suspend fun onPreShowResult(params: PreShowParams<Param>, preShowResult: PreShowResult) {}

    final override suspend fun load(params: LoadParams<Param>): LoadResult {
        return load(params.param)
    }

    final override suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {}

    protected abstract suspend fun getPreShowWhenTheFirstTime(param: Param): PreShowResult

    protected abstract suspend fun load(param: Param): LoadResult

}