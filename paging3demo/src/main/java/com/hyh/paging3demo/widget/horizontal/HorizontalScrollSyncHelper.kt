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

    fun addObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.addObserver(observer)
        scrollData?.let {
            observer.onScroll(it.scrollState, it.data)
        }
    }

    fun removeObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.deleteObserver(observer)
    }

    fun notifyScrollEvent(scrollState: ScrollState, data: Any) {
        if (scrollData == null) {
            scrollData = ScrollData(scrollState, data)
        } else {
            scrollData?.scrollState = scrollState
            scrollData?.data = scrollState
        }
        scrollData?.apply {
            scrollSyncObservable.setScrollData(this)
        }
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
    }
}


interface ScrollSyncObserver : Observer {

    @Suppress("UNCHECKED_CAST")
    override fun update(o: Observable?, arg: Any?) {
        val scrollData = arg as? ScrollData<*> ?: return
        val data = scrollData.data ?: return
        onScroll(scrollData.scrollState, data)
    }

    fun onScroll(scrollState: ScrollState, data: Any)

}


internal class ScrollData<T>(var scrollState: ScrollState, var data: T)