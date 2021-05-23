package com.hyh.tabs.adapter

import com.hyh.tabs.ITab
import com.hyh.tabs.LoadState
import com.hyh.tabs.TabInfo
import com.hyh.tabs.internal.TabData
import com.hyh.tabs.internal.TabEvent
import com.hyh.tabs.internal.UiReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.AbstractList

/**
 * TabAdapter 基类
 *
 * @author eriche
 * @data 2021/5/20
 */
internal abstract class BaseTabAdapter<Param : Any, Tab : ITab>() : ITabAdapter<Param, Tab> {

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()

    private var receiver: UiReceiver<Param>? = null
    private var tabs: List<TabInfo<Tab>>? = null

    override val tabTokens: List<Any>?
        get() = tabs?.map { it.tabToken }

    override val tabTitles: List<CharSequence>?
        get() = tabs?.map { it.tabTitle }

    private val _loadStateFlow: MutableStateFlow<LoadState> = MutableStateFlow(LoadState.Initial)

    override val loadStateFlow: Flow<LoadState>
        get() = _loadStateFlow

    override val tabCount: Int
        get() = tabs?.size ?: 0

    fun indexOf(tabInfo: TabInfo<Tab>): Int {
        return tabs?.indexOf(tabInfo) ?: -1
    }

    fun getTabInfo(position: Int): TabInfo<Tab>? {
        return tabs?.get(position)
    }

    fun getTabTitle(position: Int): CharSequence? {
        return tabs?.get(position)?.tabTitle
    }

    override suspend fun submitData(data: TabData<Param, Tab>) {
        collectFromRunner.runInIsolation {
            receiver = data.receiver
            data.flow.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is TabEvent.Loading<Tab> -> {
                            _loadStateFlow.value = LoadState.Loading
                        }
                        is TabEvent.Error<Tab> -> {
                            _loadStateFlow.value = LoadState.Error(event.error)
                        }
                        is TabEvent.Success<Tab> -> {
                            val oldTabs = tabs
                            val newTabs = event.tabs
                            tabs = newTabs
                            if (!Arrays.equals(oldTabs?.toTypedArray(), newTabs.toTypedArray())) {
                                notifyDataSetChanged()
                            }
                            _loadStateFlow.value = LoadState.Success(newTabs.size)
                        }
                    }
                }
            }
        }
    }

    override fun refresh(param: Param) {
        receiver?.refresh(param)
    }
}

internal class SingleRunner(
    cancelPreviousInEqualPriority: Boolean = true
) {
    private val holder = Holder(this, cancelPreviousInEqualPriority)

    suspend fun runInIsolation(
        priority: Int = DEFAULT_PRIORITY,
        block: suspend () -> Unit
    ) {
        try {
            coroutineScope {
                val myJob = checkNotNull(coroutineContext[Job]) {
                    "Internal error. coroutineScope should've created a job."
                }
                val run = holder.tryEnqueue(
                    priority = priority,
                    job = myJob
                )
                if (run) {
                    try {
                        block()
                    } finally {
                        holder.onFinish(myJob)
                    }
                }
            }
        } catch (cancelIsolatedRunner: CancelIsolatedRunnerException) {
            // if i was cancelled by another caller to this SingleRunner, silently cancel me
            if (cancelIsolatedRunner.runner !== this@SingleRunner) {
                throw cancelIsolatedRunner
            }
        }
    }

    /**
     * Internal exception which is used to cancel previous instance of an isolated runner.
     * We use this special class so that we can still support regular cancelation coming from the
     * `block` but don't cancel its coroutine just to cancel the block.
     */
    private class CancelIsolatedRunnerException(val runner: SingleRunner) : CancellationException()

    private class Holder(
        private val singleRunner: SingleRunner,
        private val cancelPreviousInEqualPriority: Boolean
    ) {
        private val mutex = Mutex()
        private var previous: Job? = null
        private var previousPriority: Int = 0

        suspend fun tryEnqueue(
            priority: Int,
            job: Job
        ): Boolean {
            mutex.withLock {
                val prev = previous
                return if (prev == null ||
                    !prev.isActive ||
                    previousPriority < priority ||
                    (previousPriority == priority && cancelPreviousInEqualPriority)
                ) {
                    prev?.cancel(CancelIsolatedRunnerException(singleRunner))
                    prev?.join()
                    previous = job
                    previousPriority = priority
                    true
                } else {
                    false
                }
            }
        }

        suspend fun onFinish(job: Job) {
            mutex.withLock {
                if (job === previous) {
                    previous = null
                }
            }
        }
    }

    companion object {
        const val DEFAULT_PRIORITY = 0
    }
}
