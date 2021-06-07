package com.hyh.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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