package com.hyh.list.internal.paging

import com.hyh.coroutine.SimpleMutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

internal class LoadEventHandler {

    private val state = SimpleMutableStateFlow<Pair<Int, LoadEvent>>(0 to LoadEvent.Refresh)

    private var cacheEvent: LoadEvent? = null

    val flow: Flow<LoadEvent> = state.asStateFlow().map { it.second }

    private val refreshComplete: AtomicBoolean = AtomicBoolean(false)

    private val appendComplete: AtomicBoolean = AtomicBoolean(true)

    private val rearrangeComplete: AtomicBoolean = AtomicBoolean(true)

    @Synchronized
    fun onReceiveLoadEvent(event: LoadEvent) {
        when (event) {
            LoadEvent.Refresh -> {
                if (refreshComplete.get() || state.value != event) {
                    refreshComplete.set(false)
                    appendComplete.set(true)
                    rearrangeComplete.set(true)
                    state.value = Pair(state.value.first + 1, event)
                }
            }
            LoadEvent.Append -> {
                if (refreshComplete.get() && appendComplete.get()) {
                    if (rearrangeComplete.get()) {
                        appendComplete.set(false)
                        state.value = Pair(state.value.first + 1, event)
                    } else {
                        cacheEvent = event
                    }
                }
            }
            LoadEvent.Rearrange -> {
                if (refreshComplete.get() && appendComplete.get()) {
                    rearrangeComplete.set(false)
                    state.value = Pair(state.value.first + 1, event)
                } else {
                    cacheEvent = event
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
            LoadEvent.Rearrange -> {
                rearrangeComplete.set(true)
            }
        }
        val cacheEvent = cacheEvent
        if (cacheEvent != null) {
            if (refreshComplete.get() && appendComplete.get()) {
                this.cacheEvent = null
                onReceiveLoadEvent(cacheEvent)
            }
        }
    }
}