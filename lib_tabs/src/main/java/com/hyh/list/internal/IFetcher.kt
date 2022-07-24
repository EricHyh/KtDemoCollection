package com.hyh.list.internal

/**
 * 数据拉取接口
 *
 * @author eriche 2022/7/4
 */
interface IFetcher<Param> {


    fun refreshRepo(param: Param)

    fun refreshSources(important: Boolean = false)
    fun refreshSources(vararg sourceIndexes: Int, important: Boolean = false)
    fun refreshSources(sourceIndexStart: Int, count: Int, important: Boolean = false)
    fun refreshSources(vararg sourceTokens: Any, important: Boolean = false)
    fun refreshSources(sourceTokenStart: Any, count: Int, important: Boolean = false)

    fun sourceAppend(sourceToken: Any, important: Boolean = false)
    fun sourceRearrange(sourceToken: Any, important: Boolean = false)

}