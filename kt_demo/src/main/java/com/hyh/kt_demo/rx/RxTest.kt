package com.hyh.kt_demo.rx

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
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

    /*getPageAndNext(10).subscribe {
        println("next:$it")
    }*/

    /*getPageAndNext(10)
        .subscribe {
        println("subscribe:$it")
    }*/


    get1()
        .timeout(500, TimeUnit.MILLISECONDS)
        .doOnError {
            println("doOnError1 $it")
        }
        .subscribe(
            {
                println("subscribe $it")
            },
            {
                println("doOnError2 $it")
            },
            {

            }
        )


    println("end")

    Thread.sleep(100000)
}


private fun get1(): Observable<String?> {
    /*return Observable.just(1).map {
        Thread.sleep(5000)
        "1"
    }.subscribeOn(Schedulers.io())*/
    return Observable.create<String> {
        Thread.sleep(1000)
        it.onNext("1")
        it.onComplete()
    }.observeOn(Schedulers.io())
        .doOnNext {
            println("get1 $it")
        }
}

private fun get2(): Observable<String> {
    return Observable.just(1).map {
        Thread.sleep(1000)
        "2"
    }.subscribeOn(Schedulers.io()).doOnNext {
        println("get2 $it")
    }
}


private fun getPageAndNext(page: Int): Observable<String> {
    return getResult(page).concatMap { it ->
        if (it.first <= 0) {
            Observable.just(it.second)
        } else {
            Observable.just(it.second).concatWith(getPageAndNext(it.first))
        }
    }.onErrorReturn {
        "error"
    }.reduce { t1, t2 ->
        if (t2 == "error") {
            return@reduce t2
        }
        t1 + t2
    }.toObservable()
}

private fun getResult(page: Int): Observable<Pair<Int, String>> {
    return Observable.just(page).map {
        if (page == 5) {
            throw RuntimeException("fuck")
        }
        Pair(page - 1, page.toString())
    }.doOnError {
        println("onError:")
    }
}