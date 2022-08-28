package com.hyh.list.internal.paging

import com.hyh.Invoke
import com.hyh.coroutine.cancelableChannelFlow
import com.hyh.list.ItemPagingSource
import com.hyh.list.internal.*
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.base.DispatcherProvider
import com.hyh.list.internal.base.IItemFetcherSnapshot
import com.hyh.list.internal.base.SourceResultProcessorGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PagingSourceItemRearrangeSnapshot<Param, Item>(
    private val displayedData: PagingSourceDisplayedData<Param>,
    private val loader: PagingSourceLoader<Param, Item>,
    private val onRearrangeComplete: Invoke,
    private val delegate: BaseItemSource.Delegate<ItemPagingSource.LoadParams<Param>, Item>,
    private val fetchDispatcherProvider: DispatcherProvider<ItemPagingSource.LoadParams<Param>>,
    private val processDataDispatcherProvider: DispatcherProvider<ItemPagingSource.LoadParams<Param>>,
) : IItemFetcherSnapshot {

    @Volatile
    private var closed = false
    private val sourceEventChannelFlowJob = Job()
    private val sourceEventCh = Channel<SourceEvent>(Channel.BUFFERED)


    override val sourceEventFlow: Flow<SourceEvent> =
        cancelableChannelFlow(sourceEventChannelFlowJob) {
            launch {
                sourceEventCh.consumeAsFlow().collect {
                    try {
                        if (closed) return@collect
                        send(it)
                    } catch (e: ClosedSendChannelException) {
                    }
                }
            }

            val param = ItemPagingSource.LoadParams.Rearrange(displayedData)
            val fetchDispatcher = fetchDispatcherProvider.invoke(param, displayedData)

            val loadResult: ItemPagingSource.LoadResult<Param, Item> =
                if (fetchDispatcher == null) {
                    loader.invoke(param)
                } else {
                    withContext(fetchDispatcher) {
                        loader.invoke(param)
                    }
                }

            when (loadResult) {
                is ItemPagingSource.LoadResult.Error -> {
                    onRearrangeComplete()
                }
                is ItemPagingSource.LoadResult.Rearranged -> {
                    if (loadResult.ignore) {
                        onRearrangeComplete()
                    } else {
                        SourceEvent.PagingRearrangeSuccess(
                            rearrangeProcessor(param, loadResult),
                        ) {
                            onRearrangeComplete()
                        }.apply {
                            sourceEventCh.send(this)
                        }
                    }
                }
                else -> {
                    throw IllegalArgumentException("loadResult shouldn't be $loadResult")
                }
            }

        }

    override fun close() {
        closed = true
        sourceEventChannelFlowJob.cancel()
    }

    private fun rearrangeProcessor(
        param: ItemPagingSource.LoadParams<Param>,
        rearranged: ItemPagingSource.LoadResult.Rearranged<Param, Item>
    ): SourceResultProcessor {
        return SourceResultProcessorGenerator(
            sourceDisplayedData = displayedData,
            items = rearranged.items,
            resultExtra = rearranged.resultExtra,
            onResultDisplayed = rearranged.onResultDisplayed,
            dispatcher = processDataDispatcherProvider.invoke(param, displayedData),
            delegate = delegate
        ).processor
    }
}