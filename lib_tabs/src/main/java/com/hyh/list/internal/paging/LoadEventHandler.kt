package com.hyh.list.internal.paging

import com.hyh.coroutine.SimpleMutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

internal class LoadEventHandler {

    private val state = SimpleMutableStateFlow<Pair<Int, LoadEvent>>(0 to LoadEvent.Refresh)

    private var cacheRefreshEvent: LoadEvent? = null

    private var cacheAppendEvent: LoadEvent? = null

    private var cacheRearrangeEvent: LoadEvent? = null

    val flow: Flow<LoadEvent> = state.asStateFlow().map { it.second }

    private val refreshComplete: AtomicBoolean = AtomicBoolean(false)

    private val appendComplete: AtomicBoolean = AtomicBoolean(true)

    private val rearrangeComplete: AtomicBoolean = AtomicBoolean(true)

    @Synchronized
    fun onReceiveLoadEvent(event: LoadEvent, important: Boolean = false) {
        when (event) {
            LoadEvent.Refresh -> {
                if (refreshComplete.get() || state.value != event) {
                    refreshComplete.set(false)
                    appendComplete.set(true)
                    rearrangeComplete.set(true)
                    state.value = Pair(state.value.first + 1, event)
                } else if (important) {
                    cacheRefreshEvent = event
                }
            }
            LoadEvent.Append -> {
                if (refreshComplete.get() && appendComplete.get()) {
                    if (rearrangeComplete.get()) {
                        appendComplete.set(false)
                        state.value = Pair(state.value.first + 1, event)
                    } else {
                        cacheAppendEvent = event
                    }
                } else if (important) {
                    cacheAppendEvent = event
                }
            }
            LoadEvent.Rearrange -> {
                if (rearrangeComplete.get()) {
                    rearrangeComplete.set(false)
                    state.value = Pair(state.value.first + 1, event)
                } else {
                    cacheRearrangeEvent = event
                }
            }
        }
    }

    @Synchronized
    fun onLoadEventComplete(event: LoadEvent) {
        when (event) {
            LoadEvent.Refresh -> {
                refreshComplete.set(true)
                appendComplete.set(true)
                rearrangeComplete.set(true)

                handleCacheRefreshEvent()
                handleCacheAppendEvent()
            }
            LoadEvent.Append -> {
                appendComplete.set(true)
                handleCacheAppendEvent()
            }
            LoadEvent.Rearrange -> {
                rearrangeComplete.set(true)
                appendComplete.set(true)

                handleCacheRearrangeEvent()
                handleCacheAppendEvent()
            }
        }
    }

    private fun handleCacheRefreshEvent() {
        val cacheRefreshEvent = this.cacheRefreshEvent
        if (cacheRefreshEvent != null && refreshComplete.get()) {
            this.cacheRefreshEvent = null
            onReceiveLoadEvent(cacheRefreshEvent)
        }
    }

    private fun handleCacheAppendEvent() {
        val cacheAppendEvent = this.cacheAppendEvent
        if (cacheAppendEvent != null
            && refreshComplete.get()
            && appendComplete.get()
            && rearrangeComplete.get()
        ) {
            this.cacheAppendEvent = null
            onReceiveLoadEvent(cacheAppendEvent)
        }
    }

    private fun handleCacheRearrangeEvent() {
        val cacheRearrangeEvent = this.cacheRearrangeEvent
        if (cacheRearrangeEvent != null && rearrangeComplete.get()) {
            this.cacheRearrangeEvent = null
            onReceiveLoadEvent(cacheRearrangeEvent)
        }
    }
}