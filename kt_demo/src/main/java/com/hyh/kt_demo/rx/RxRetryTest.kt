package com.hyh.kt_demo.rx

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/9/24
 */
fun main() {

    println("start")


    val create = PublishSubject.create<Unit>()
    Thread {
        Thread.sleep(2000)
        create.onNext(Unit)
        //create.onComplete()
        println("onNext")
    }.start()


    create.takeLast(0).subscribe {
        println("subscribe1 $it")
    }

    create.subscribe {
        println("subscribe2 $it")
    }

    Observable.just(1)
        .map { t ->
            println("map:$t")
            t
        }
        .flatMap {
            Observable.just(10).map {
                println("inner map: it")
                throw NullPointerException()
                it
            }.retryWhen(RetryHandler()).compose(LifecycleTransformer(create))
        }
        .doOnDispose {
            println("doOnDispose1")
        }
        .compose(LifecycleTransformer(create))
        .doOnDispose {
            println("doOnDispose2")
        }
        .subscribe {
            println("subscribe - $it")
        }


    MutableStateFlow<Int>(0).asStateFlow()

    /* getConcatPageObservable(0)
         .reduce { t1: String?, t2: String? ->
             "$t1,$t2"
         }
         .toObservable()
         .retryWhen(RetryHandler())
         .onErrorReturn {
             "-1"
         }
         .subscribe {
             println("subscribe:$it")
         }*/


    /*val subject: BehaviorSubject<Int> = BehaviorSubject.create<Int>()

    Observable.just(1).map {
        it
    }.subscribe(object : Observer<Int> {
        override fun onSubscribe(d: Disposable) {
            println("Observable:onSubscribe")
        }

        override fun onNext(baseMsgType: Int) {
            subject.onNext(baseMsgType)
            println("Observable:onNext")
        }

        override fun onComplete() {

            println("Observable:onComplete")
        }

        override fun onError(e: Throwable) {
            subject.onError(e)
            println("Observable:onError")
        }
    })

    Thread {
        Thread.sleep(2000)
        subject.subscribe(object : Observer<Int> {
            override fun onSubscribe(d: Disposable) {
                println("subject:onSubscribe")
            }

            override fun onNext(baseMsgType: Int) {
                println("subject:onNext $baseMsgType")
                subject.onComplete()
            }

            override fun onComplete() {
                println("subject:onComplete")
            }

            override fun onError(e: Throwable) {
                println("subject:onError $e")
            }
        })
    }.start()*/


    Thread.sleep(10000000)
}

//ObservableTransformer<? super T, ? extends R>

var count = 0

fun getConcatPageObservable(page: Int): Observable<String> {
    return getPageObservable(page).concatMap {
        if (it == 5) {
            Observable.just("$it")
        } else {
            Observable.just("$it").concatWith(getConcatPageObservable(it + 1))
        }
    }
}

fun getPageObservable(page: Int): Observable<Int> {
    return Observable.just(page)
        .map {
            if (it == 3 && count <= 10) throw NullPointerException()
            it
        }

}


class LifecycleTransformer<T>(private val observable: Observable<*>) : ObservableTransformer<T, T> {

    override fun apply(upstream: Observable<T>): ObservableSource<T> {
        return upstream.takeUntil(observable)
    }

}


class RetryHandler :
    Function<Observable<Throwable>, ObservableSource<*>> {

    override fun apply(attempts: Observable<Throwable>): ObservableSource<*> {
        return attempts.flatMap {
            count++
            Observable.timer(
                100L,
                TimeUnit.MILLISECONDS
            )
        }
    }
}