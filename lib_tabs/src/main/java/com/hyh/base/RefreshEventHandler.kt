package com.hyh.base

import android.os.SystemClock
import android.util.Log
import com.hyh.coroutine.SimpleMutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * 接收事件，触发数据加载
 *
 * @author eriche
 * @data 2021/6/29
 */
abstract class RefreshEventHandler<Param>(initialParam: Param) {


    companion object {
        private const val TAG = "RefreshEventHandler"
        private const val MAX_BLOCK_TIME_MILLIS = 5 * 1000
    }

    //private val state = MutableStateFlow<Pair<Long, Param?>>(Pair(0, initialParam))
    private val state = SimpleMutableStateFlow<Pair<Long, Param>>(Pair(0, initialParam))

    //private var cacheState: MutableStateFlow<Pair<Long, Param>>? = null
    private var cacheState: SimpleMutableStateFlow<Pair<Long, Param>>? = null

    private var loadStage = RefreshStage.UNBLOCK
        set(value) {
            field = value
            if (field == RefreshStage.BLOCK) {
                this.blockTimeStart = SystemClock.elapsedRealtime()
            } else {
                this.blockTimeStart = 0
            }
        }
    private var blockTimeStart: Long = 0

    private var delay = 0
    private var timingStart: Long = 0

    val flow = state.asStateFlow().map { it.second }

    @Synchronized
    fun onReceiveRefreshEvent(important: Boolean, param: Param) {
        if (important) {
            loadStage = RefreshStage.BLOCK
            state.value = Pair(state.value.first + 1, param)
            this.cacheState = null
            this.timingStart = 0
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
                if (abs(SystemClock.elapsedRealtime() - blockTimeStart) > MAX_BLOCK_TIME_MILLIS) {
                    Log.i(TAG, "block over MAX_BLOCK_TIME_MILLIS")
                    removeBlockIfNecessary()
                }
            }
        }
    }


    @Synchronized
    fun onRefreshComplete() {
        removeBlockIfNecessary()
    }

    fun onDestroy() {
        state.close()
        cacheState?.close()
    }

    abstract fun getRefreshStrategy(): RefreshStrategy

    private fun removeBlockIfNecessary() {
        if (this.loadStage == RefreshStage.BLOCK) {
            val cacheState = this.cacheState
            this.cacheState = null
            this.timingStart = 0
            this.loadStage = RefreshStage.UNBLOCK
            if (cacheState != null) {
                onReceiveRefreshEvent(false, cacheState.value.second)
                cacheState.close()
            }
        }
    }

}

/**
 * 刷新策略的阶段
 */
enum class RefreshStage {
    /**
     * 未阻塞阶段，该阶段一律会接收外部的刷新事件
     */
    UNBLOCK,

    /**
     * 计时阶段，该阶段一律会接收外部的刷新事件，同时会计算处于该阶段的时长，超过指定时长则进入[BLOCK]阶段
     */
    TIMING,

    /**
     * 阻塞阶段，在本次刷新完成之前，不会处理后续的刷新事件，但是会缓存最近一次的刷新事件
     * 在刷新完成时会将状态转换为[UNBLOCK]，同时会继续处理缓存的刷新事件
     */
    BLOCK
}

/**
 * 刷新策略
 */
sealed class RefreshStrategy {

    /**
     * 收到新的刷新事件，一律取消当前正在进行的刷新操作
     */
    object CancelLast : RefreshStrategy()

    /**
     * 排队等待，只有当前刷新操作完成后，才处理后续的刷新事件
     */
    object QueueUp : RefreshStrategy()

    /**
     * 延迟等待，在一定时间内采取[CancelLast]策略，如果这段时间一直没有完成刷新，则采取[QueueUp]策略
     *
     * 这是为了解决下面描述的这种场景：
     * 一般情况下我想按照[CancelLast]策略执行刷新策略，但是由于某种极端原因，可能会出现一段时间内连续执行刷新事件，
     * 这会导致每次刷新都会被下一次的刷新事件取消，从而出现刷新不及时的情况，采用[DelayedQueueUp]策略可以有效的解决
     * 这种情况。
     *
     * @property delay 毫秒
     */
    data class DelayedQueueUp(val delay: Int) : RefreshStrategy()

}