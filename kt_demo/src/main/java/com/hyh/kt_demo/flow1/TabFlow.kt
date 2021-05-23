package com.hyh.kt_demo.flow1

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume


/**
 * Temporary `scan` operator on Flow without experimental APIs.
 */
internal fun <T, R> Flow<T>.simpleScan(
    initial: R,
    operation: suspend (accumulator: R, value: T) -> R
): Flow<R> = flow {
    var accumulator: R = initial
    emit(accumulator)
    collect { value ->
        accumulator = operation(accumulator, value)
        emit(accumulator)
    }
}

/**
 * mapLatest without experimental APIs
 */
internal inline fun <T, R> Flow<T>.simpleMapLatest(
    crossinline transform: suspend (value: T) -> R
): Flow<R> = simpleTransformLatest { emit(transform(it)) }

/**
 * This is a similar implementation to transformLatest using a channel Flow.
 */
internal fun <T, R> Flow<T>.simpleTransformLatest(
    transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = simpleChannelFlow {
    val origin = this@simpleTransformLatest
    val collector = ChannelFlowCollector(this@simpleChannelFlow)
    origin.collectLatest { value ->
        collector.transform(value)
    }
}

internal fun <T> cancelableChannelFlow(
    controller: Job,
    block: suspend SimpleProducerScope<T>.() -> Unit
): Flow<T> {
    return simpleChannelFlow<T> {
        controller.invokeOnCompletion {
            close()
        }
        this.block()
    }
}

internal fun <T> simpleChannelFlow(
    block: suspend SimpleProducerScope<T>.() -> Unit
): Flow<T> {
    return flow {
        coroutineScope {
            val channel = Channel<T>(capacity = Channel.RENDEZVOUS)
            val producer = launch {
                try {
                    // run producer in a separate inner scope to ensure we wait for its children
                    // to finish, in case it does more launches inside.
                    coroutineScope {
                        val producerScopeImpl = SimpleProducerScopeImpl(
                            scope = this,
                            channel = channel,
                        )
                        producerScopeImpl.block()
                    }
                    channel.close()
                } catch (t: Throwable) {
                    channel.close(t)
                }
            }
            for (item in channel) {
                emit(item)
            }
            // in case channel closed before producer completes, cancel the producer.
            producer.cancel()
        }
    }.buffer(Channel.BUFFERED)
}

internal interface SimpleProducerScope<T> : CoroutineScope, SendChannel<T> {
    val channel: SendChannel<T>
    suspend fun awaitClose(block: () -> Unit)
}

internal class SimpleProducerScopeImpl<T>(
    scope: CoroutineScope,
    override val channel: SendChannel<T>,
) : SimpleProducerScope<T>, CoroutineScope by scope, SendChannel<T> by channel {
    override suspend fun awaitClose(block: () -> Unit) {
        try {
            val job = checkNotNull(coroutineContext[Job]) {
                "Internal error, context should have a job."
            }
            suspendCancellableCoroutine<Unit> { cont ->
                job.invokeOnCompletion {
                    cont.resume(Unit)
                }
            }
        } finally {
            block()
        }
    }
}

internal class ChannelFlowCollector<T>(
    private val channel: SendChannel<T>
) : FlowCollector<T> {
    override suspend fun emit(value: T) {
        channel.send(value)
    }
}