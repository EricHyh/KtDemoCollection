package com.hyh.kt_demo.rx

import io.reactivex.rxjava3.core.Observable
import java.lang.RuntimeException

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/9/24
 */
fun main() {


    println("start")

    getPageAndNext(10).subscribe {
        println("next:$it")
    }

    /*getPageAndNext(10)
        .subscribe {
        println("subscribe:$it")
    }*/

    println("end")
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