package com.hyh.list

abstract class SimpleItemSource<Param : Any> : IItemSource<Param> {

    override suspend fun getPreShow(params: PreShowParams<Param>): IItemSource.PreShowResult {
        return if (params.lastLoadResult == null && params.lastPreShowResult == null) {
            getPreShowJustFirstTime(params.param)
        } else {
            IItemSource.PreShowResult.Unused
        }
    }

    override suspend fun load(params: LoadParams<Param>): IItemSource.LoadResult {
        return load(params.param)
    }

    protected abstract suspend fun getPreShowJustFirstTime(param: Param): IItemSource.PreShowResult

    protected abstract suspend fun load(param: Param): IItemSource.LoadResult

}