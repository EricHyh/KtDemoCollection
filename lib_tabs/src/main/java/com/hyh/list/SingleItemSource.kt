package com.hyh.list

/**
 * 单Item数据源
 *
 * @author eriche 2021/9/29
 */
class SingleItemSource(override val sourceToken: Any, private val item: FlatListItem) : SimpleItemSource<Unit>(Unit) {

    constructor(item: FlatListItem) : this(item.javaClass, item)

    override suspend fun getPreShow(param: Unit): PreShowResult<FlatListItem> {
        return PreShowResult.Success(listOf(item))
    }

    override suspend fun load(param: Unit): LoadResult<FlatListItem> {
        return LoadResult.Success(listOf(item))
    }

    override suspend fun getParam() {
    }
}