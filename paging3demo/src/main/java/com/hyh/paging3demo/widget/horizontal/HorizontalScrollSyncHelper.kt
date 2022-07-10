package com.hyh.paging3demo.widget.horizontal

import java.util.*
import kotlin.collections.HashSet

/**
 * 水平滑动状态同步工具类
 *
 * @author eriche 2021/12/28
 */
class HorizontalScrollSyncHelper {

    private var scrollDataWrapper: ScrollDataWrapper = ScrollDataWrapper(ScrollState.INITIAL, Unit)

    private val scrollSyncObservable = ScrollSyncObservable()

    private val actionDownPublishers: MutableSet<ScrollSyncObserver> = HashSet()

    internal fun addObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.addObserver(observer)
        observer.onScroll(scrollDataWrapper.scrollState, scrollDataWrapper.data)
    }

    internal fun removeObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.deleteObserver(observer)
        actionDownPublishers.remove(observer)
    }

    internal fun sync(observer: ScrollSyncObserver) {
        observer.onScroll(scrollDataWrapper.scrollState, scrollDataWrapper.data)
    }

    internal fun isAllowReleaseDrag(observer: ScrollSyncObserver): Boolean {
        if (actionDownPublishers.size <= 0) return true
        if (actionDownPublishers.size == 1 && actionDownPublishers.contains(observer)) return true
        return false
    }

    internal fun notifyScrollEvent(
        scrollState: ScrollState,
        data: Any
    ) {
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


    internal fun notifyActionDown(publisher: ScrollSyncObserver) {
        actionDownPublishers.add(publisher)
        scrollSyncObservable.stopScroll()
    }

    internal fun notifyActionCancel(publisher: ScrollSyncObserver) {
        actionDownPublishers.remove(publisher)
    }

    internal inner class ScrollSyncObservable {

        private val observers: MutableCollection<ScrollSyncObserver> = Vector()

        fun addObserver(o: ScrollSyncObserver) {
            if (observers.contains(o)) return
            observers.add(o)
        }

        fun deleteObserver(o: ScrollSyncObserver) {
            observers.remove(o)
        }

        fun setScrollData(scrollDataWrapper: ScrollDataWrapper) {
            notifyObservers(scrollDataWrapper)
        }

        fun stopScroll() {
            notifyObservers(StopScroll)
        }

        private fun notifyObservers(arg: Any?) {
            observers.forEach {
                it.update(arg)
            }
        }
    }
}

interface ScrollSyncObserver {

    @Suppress("UNCHECKED_CAST")
    fun update(arg: Any?) {
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