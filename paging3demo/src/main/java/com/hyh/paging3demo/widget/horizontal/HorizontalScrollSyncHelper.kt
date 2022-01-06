package com.hyh.paging3demo.widget.horizontal

import java.util.*

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/28
 */
class HorizontalScrollSyncHelper {

    private var scrollData: ScrollData<Any>? = null

    private val scrollSyncObservable = ScrollSyncObservable()

    internal fun addObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.addObserver(observer)
        scrollData?.let {
            observer.onScroll(it.scrollState, it.data)
        }
    }

    internal fun removeObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.deleteObserver(observer)
    }

    internal fun notifyScrollEvent(scrollState: ScrollState, data: Any) {
        if (scrollData == null) {
            scrollData = ScrollData(scrollState, data)
        } else {
            scrollData?.scrollState = scrollState
            scrollData?.data = data
        }
        scrollData?.apply {
            scrollSyncObservable.setScrollData(this)
        }
    }

    internal fun notifyStopScroll() {
        scrollSyncObservable.stopScroll()
    }

    internal inner class ScrollSyncObservable : Observable() {

        private var scrollData: ScrollData<Any>? = null

        fun setScrollData(scrollData: ScrollData<*>) {
            if (this.scrollData == scrollData) return
            if (this.scrollData == null) {
                this.scrollData = ScrollData(scrollData.scrollState, scrollData.data as Any)
            } else {
                this.scrollData?.scrollState = scrollData.scrollState
                this.scrollData?.data = scrollData.data as Any
            }
            setChanged()
            notifyObservers(this.scrollData)
        }

        fun stopScroll() {
            setChanged()
            notifyObservers(StopScroll)
        }
    }
}

interface ScrollSyncObserver : Observer {

    @Suppress("UNCHECKED_CAST")
    override fun update(o: Observable?, arg: Any?) {
        when (arg) {
            is ScrollData<*> -> {
                val data = arg.data ?: return
                onScroll(arg.scrollState, data)
            }
            is StopScroll -> {
                onStopScroll()
            }
        }
    }

    fun onScroll(scrollState: ScrollState, data: Any)

    fun onStopScroll()

}


internal class ScrollData<T>(var scrollState: ScrollState, var data: T)

internal object StopScroll