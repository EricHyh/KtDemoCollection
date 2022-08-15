package com.hyh.kt_demo.flow3

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/31
 */

fun main() {

    val flow = channelFlow {
        send(0)
    }

    val observable = Observable.create<Int> {
        it.onNext(1)
        it.onNext(2)
        it.onComplete()
    }



    runBlocking {
        observable.xxFlow().collect {
            println("$it")
        }
    }

    println("xx")
}


/*
fun <T : Any> Observable<T>.asFlow(): Flow<T> {
    flow<T> {

    }
}

*/


public fun <T : Any> ObservableSource<T>.xxFlow(): Flow<T> = callbackFlow {
    val disposableRef = AtomicReference<Disposable>()
    val observer = object : Observer<T> {
        override fun onComplete() { close() }

        override fun onSubscribe(d: Disposable) {
            if (!disposableRef.compareAndSet(null, d)) d.dispose()
        }

        override fun onNext(t: T) {
            /*
             * Channel was closed by the downstream, so the exception (if any)
             * also was handled by the same downstream
             */
            try {
                trySendBlocking(t)
            } catch (e: InterruptedException) {
                // RxJava interrupts the source
            }
        }

        override fun onError(e: Throwable) {
            close(e)
        }
    }

    subscribe(observer)
    awaitClose {
        println("awaitClose")
        disposableRef.getAndSet(Disposable.disposed())?.dispose()
    }
}