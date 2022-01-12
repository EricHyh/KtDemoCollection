package com.hyh.paging3demo.widget.horizontal

import java.util.*

/**
 * 水平滑动状态同步工具类
 *
 * @author eriche 2021/12/28
 */
class HorizontalScrollSyncHelper {

    companion object {
        private const val TAG = "ScrollSyncHelper"
    }

    private var scrollDataWrapper: ScrollDataWrapper = ScrollDataWrapper(ScrollState.INITIAL, Unit)

    private val scrollSyncObservable = ScrollSyncObservable()

    internal fun addObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.addObserver(observer)
        observer.onScroll(scrollDataWrapper.scrollState, scrollDataWrapper.data)
    }

    internal fun sync(observer: ScrollSyncObserver) {
        observer.onScroll(scrollDataWrapper.scrollState, scrollDataWrapper.data)
    }

    internal fun removeObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.deleteObserver(observer)
    }

    internal fun notifyScrollEvent(scrollState: ScrollState, data: Any) {
        if (scrollState == scrollDataWrapper.scrollState && data == scrollDataWrapper.data) return
        scrollDataWrapper.scrollState = scrollState
        scrollDataWrapper.data = when (scrollState) {
            ScrollState.IDLE, ScrollState.SCROLL, ScrollState.SETTLING -> {
                val newScrollData = data as IScrollData
                val oldScrollData = scrollDataWrapper.data
                if (oldScrollData is IScrollData) {
                    oldScrollData.copy(newScrollData)
                    oldScrollData
                } else {
                    newScrollData.clone()
                }
            }
            else -> {
                data
            }
        }

        scrollSyncObservable.setScrollData(scrollDataWrapper)
    }

    internal fun notifyStopScroll() {
        scrollSyncObservable.stopScroll()
    }

    internal inner class ScrollSyncObservable : Observable() {

        fun setScrollData(scrollDataWrapper: ScrollDataWrapper) {
            setChanged()
            notifyObservers(scrollDataWrapper)
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
            is ScrollDataWrapper -> {
                val data = arg.data
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

internal data class ScrollDataWrapper(var scrollState: ScrollState, var data: Any)

internal object StopScroll