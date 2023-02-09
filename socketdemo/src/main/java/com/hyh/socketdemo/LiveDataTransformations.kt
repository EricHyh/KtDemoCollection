package com.hyh.socketdemo

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import io.reactivex.Observable

/**
 * [LiveData]转换工具
 *
 * @author eriche 2022/12/26
 */
object LiveDataTransformations {

    @MainThread
    fun <X, Y> mapWithValue(
        source: LiveData<X>,
        mapFunction: Function<X, Y>
    ): LiveData<Y> {
        val result = MediatorLiveData<Y>().apply {
            observeForever {}
        }
        result.addSource(source) { x -> result.value = mapFunction.apply(x) }
        return result
    }

    @MainThread
    fun <X> distinctWithValue(source: LiveData<X>): LiveData<X> {
        val outputLiveData = MediatorLiveData<X>().apply {
            observeForever {}
        }
        outputLiveData.addSource(source, object : Observer<X> {
            var mFirstTime = true
            override fun onChanged(currentValue: X) {
                val previousValue = outputLiveData.value
                if (mFirstTime
                    || previousValue == null && currentValue != null
                    || previousValue != null && previousValue != currentValue
                ) {
                    mFirstTime = false
                    outputLiveData.value = currentValue
                }
            }
        })
        return outputLiveData
    }


}

inline fun <X, Y> LiveData<X>.mapWithValue(crossinline transform: (X) -> Y): LiveData<Y> =
    LiveDataTransformations.mapWithValue(this) { transform(it) }

@Suppress("NOTHING_TO_INLINE")
inline fun <X> LiveData<X>.distinctWithValue(): LiveData<X> =
    LiveDataTransformations.distinctWithValue(this)
