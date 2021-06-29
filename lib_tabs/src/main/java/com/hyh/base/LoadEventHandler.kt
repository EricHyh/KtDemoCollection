package com.hyh.base

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * 接收事件，触发数据加载
 *
 * @author eriche
 * @data 2021/6/29
 */
abstract class BaseLoadEventHandler<Param : Any>(initialParam: Param?) {

    private val state = MutableStateFlow<Pair<Long, Param?>>(Pair(0, initialParam))

    private var cacheState: MutableStateFlow<Pair<Long, Param>>? = null
    private var loadStage = LoadStage.UNBLOCK
    private var delay = 0
    private var timingStart: Long = 0

    val flow = state.asStateFlow()

    @Synchronized
    fun onReceiveLoadEvent(important: Boolean, param: Param) {
        if (important) {
            state.value = Pair(state.value.first + 1, param)
            this.cacheState = null
            this.timingStart = 0
            loadStage = LoadStage.BLOCK
            return
        }
        when (loadStage) {
            LoadStage.UNBLOCK -> {
                state.value = Pair(state.value.first + 1, param)
                when (val refreshStrategy = getLoadStrategy()) {
                    is LoadStrategy.QueueUp -> {
                        loadStage = LoadStage.BLOCK
                    }
                    is LoadStrategy.DelayedQueueUp -> {
                        loadStage = LoadStage.TIMING
                        timingStart = SystemClock.elapsedRealtime()
                        delay = refreshStrategy.delay
                    }
                    else -> {
                    }
                }
            }
            LoadStage.TIMING -> {
                state.value = Pair(state.value.first + 1, param)
                val elapsedRealtime = SystemClock.elapsedRealtime()
                if (abs(elapsedRealtime - timingStart) > delay) {
                    loadStage = LoadStage.BLOCK
                }

            }
            LoadStage.BLOCK -> {
                val cacheState = this.cacheState
                if (cacheState != null) {
                    cacheState.value = Pair(cacheState.value.first + 1, param)
                } else {
                    this.cacheState = MutableStateFlow(Pair(0L, param))
                }
            }
        }
    }


    @Synchronized
    fun onLoadComplete() {
        val cacheState = this.cacheState
        this.cacheState = null
        this.timingStart = 0
        this.loadStage = LoadStage.UNBLOCK
        if (cacheState != null) {
            onReceiveLoadEvent(false, cacheState.value.second)
        }
    }

    abstract fun getLoadStrategy(): LoadStrategy

}

enum class LoadStage {
    UNBLOCK,
    TIMING,
    BLOCK
}

sealed class LoadStrategy {

    object CancelLast : LoadStrategy()
    object QueueUp : LoadStrategy()
    data class DelayedQueueUp(val delay: Int) : LoadStrategy()

}