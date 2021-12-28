package com.hyh.paging3demo.widget.horizontal

import java.util.*

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/28
 */
class HorizontalScrollSyncHelper {


    fun addObserver(observer: ScrollSyncObserver) {

    }

    fun removeObserver(observer: ScrollSyncObserver) {

    }

    inner class ScrollSyncObservable : Observable() {

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
        onScroll(data, scrollData.scrollState)
    }

    fun onScroll(data: Any, scrollState: ScrollState)

}


class ScrollData<T>(var scrollState: ScrollState, var data: T)