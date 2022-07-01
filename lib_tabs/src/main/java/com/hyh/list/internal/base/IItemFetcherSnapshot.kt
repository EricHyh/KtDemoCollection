package com.hyh.list.internal.base

import com.hyh.list.internal.SourceEvent
import kotlinx.coroutines.flow.Flow

/**
 * Item数据拉取处理类
 *
 * @author eriche 2022/7/1
 */
interface IItemFetcherSnapshot {

    val sourceEventFlow: Flow<SourceEvent>

    fun close()

}