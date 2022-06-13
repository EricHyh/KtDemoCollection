package com.hyh.list.internal

import com.hyh.coroutine.SimpleMutableStateFlow
import com.hyh.list.ItemPagingSource
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicBoolean

class PagingSourceFetcher<Param : Any, Item : Any>(
    pagingSource: ItemPagingSource<Param, Item>
) {





}


internal class LoadEventHandler {

    private val state = SimpleMutableStateFlow<LoadEvent>(LoadEvent.Refresh)

    val flow: Flow<LoadEvent> = state.asStateFlow()

    private val refreshComplete: AtomicBoolean = AtomicBoolean(false)

    private val appendComplete: AtomicBoolean = AtomicBoolean(false)

    @Synchronized
    fun onReceiveLoadEvent(event: LoadEvent) {
        when (event) {
            LoadEvent.Refresh -> {
                if (refreshComplete.get() || state.value != event) {
                    refreshComplete.set(false)
                    state.value = event
                }
            }
            LoadEvent.Append -> {
                if (appendComplete.get() && refreshComplete.get()) {
                    appendComplete.set(false)
                    state.value = event
                }
            }
        }
    }

    @Synchronized
    fun onLoadEventComplete(event: LoadEvent) {
        when (event) {
            LoadEvent.Refresh -> {
                refreshComplete.set(true)
            }
            LoadEvent.Append -> {
                appendComplete.set(true)
            }
        }
    }
}


sealed class LoadEvent {

    object Refresh : LoadEvent()

    object Append : LoadEvent()

}