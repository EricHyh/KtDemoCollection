package com.hyh.base

import android.os.SystemClock
import com.hyh.coroutine.SimpleMutableStateFlow
import kotlin.math.abs

/**
 * 接收事件，触发数据加载
 *
 * @author eriche
 * @data 2021/6/29
 */
abstract class RefreshEventHandler<Param : Any>(initialParam: Param?) {

    //private val state = MutableStateFlow<Pair<Long, Param?>>(Pair(0, initialParam))
    private val state = SimpleMutableStateFlow<Pair<Long, Param?>>(Pair(0, initialParam))

    //private var cacheState: MutableStateFlow<Pair<Long, Param>>? = null
    private var cacheState: SimpleMutableStateFlow<Pair<Long, Param>>? = null

    private var loadStage = RefreshStage.UNBLOCK
    private var delay = 0
    private var timingStart: Long = 0

    val flow = state.asStateFlow()


    @Synchronized
    fun onReceiveRefreshEvent(important: Boolean, param: Param) {
        if (important) {
            state.value = Pair(state.value.first + 1, param)
            this.cacheState = null
            this.timingStart = 0
            loadStage = RefreshStage.BLOCK
            return
        }
        when (loadStage) {
            RefreshStage.UNBLOCK -> {
                state.value = Pair(state.value.first + 1, param)
                when (val refreshStrategy = getRefreshStrategy()) {
                    is RefreshStrategy.QueueUp -> {
                        loadStage = RefreshStage.BLOCK
                    }
                    is RefreshStrategy.DelayedQueueUp -> {
                        loadStage = RefreshStage.TIMING
                        timingStart = SystemClock.elapsedRealtime()
                        delay = refreshStrategy.delay
                    }
                    else -> {
                    }
                }
            }
            RefreshStage.TIMING -> {
                state.value = Pair(state.value.first + 1, param)
                val elapsedRealtime = SystemClock.elapsedRealtime()
                if (abs(elapsedRealtime - timingStart) > delay) {
                    loadStage = RefreshStage.BLOCK
                }

            }
            RefreshStage.BLOCK -> {
                val cacheState = this.cacheState
                if (cacheState != null) {
                    cacheState.value = Pair(cacheState.value.first + 1, param)
                } else {
                    this.cacheState = SimpleMutableStateFlow(Pair(0L, param))
                    //this.cacheState = MutableStateFlow(Pair(0L, param))
                }
            }
        }
    }


    @Synchronized
    fun onRefreshComplete() {
        val cacheState = this.cacheState
        this.cacheState = null
        this.timingStart = 0
        this.loadStage = RefreshStage.UNBLOCK
        if (cacheState != null) {
            onReceiveRefreshEvent(false, cacheState.value.second)
            cacheState.close()
        }
    }

    fun onDestroy() {
        state.close()
        cacheState?.close()
    }

    abstract fun getRefreshStrategy(): RefreshStrategy

}

enum class RefreshStage {
    UNBLOCK,
    TIMING,
    BLOCK
}

sealed class RefreshStrategy {

    object CancelLast : RefreshStrategy()
    object QueueUp : RefreshStrategy()
    data class DelayedQueueUp(val delay: Int) : RefreshStrategy()

}