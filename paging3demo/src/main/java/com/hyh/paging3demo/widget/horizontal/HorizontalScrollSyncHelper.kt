package com.hyh.paging3demo.widget.horizontal

import android.util.Log
import com.hyh.paging3demo.widget.horizontal.internal.IScrollData
import com.hyh.paging3demo.widget.horizontal.internal.RecyclerViewScrollable
import java.util.*
import kotlin.collections.HashSet

/**
 * 水平滑动状态同步工具类
 *
 * @author eriche 2021/12/28
 */
class HorizontalScrollSyncHelper {

    private var scrollDataWrapper: ScrollDataWrapper = ScrollDataWrapper(ScrollState.INITIAL, Unit)

    private var scrollDataWrapperPool = ScrollDataWrapperPool()

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

        if (scrollDataWrapper.notifying) {
            val obtain = scrollDataWrapperPool.obtain(scrollState, data)
            scrollSyncObservable.setScrollData(obtain)
            scrollDataWrapperPool.release(obtain)
        } else {
            scrollDataWrapper.notifying = true
            scrollDataWrapper.scrollState = scrollState
            scrollDataWrapper.data = when (scrollState) {
                ScrollState.IDLE, ScrollState.SCROLL, ScrollState.SETTLING -> {
                    val newScrollData = data as IScrollData
                    val oldScrollData = scrollDataWrapper.data
                    if (oldScrollData is IScrollData && oldScrollData.copy(newScrollData)) {
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
            scrollDataWrapper.notifying = false
        }

    }

    internal fun notifyActionDown(publisher: ScrollSyncObserver) {
        actionDownPublishers.add(publisher)
        scrollSyncObservable.stopScroll()
    }

    internal fun notifyActionCancel(publisher: ScrollSyncObserver) {
        actionDownPublishers.remove(publisher)
    }

    private class ScrollDataWrapperPool {

        private val pool: MutableList<ScrollDataWrapper> = mutableListOf()

        fun obtain(
            scrollState: ScrollState,
            data: Any
        ): ScrollDataWrapper {
            return pool.removeFirstOrNull()?.also {
                it.scrollState = scrollState
                it.data = data
            } ?: ScrollDataWrapper(scrollState, data)
        }

        fun release(scrollDataWrapper: ScrollDataWrapper) {
            pool.add(scrollDataWrapper.also {
                it.scrollState = ScrollState.INITIAL
                it.data = Unit
            })
        }

    }

    internal inner class ScrollSyncObservable {

        private val TAG = "SyncObservable"

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
            val log =
                arg is ScrollDataWrapper && arg.data is RecyclerViewScrollable.RecyclerViewScrollData.ScrolledData

            if (log) {
                Log.d(TAG, "notifyObservers start: ${observers.size}")
            }

            observers.forEach {
                it.update(arg)
                if (log) {
                    Log.d(TAG, "notifyObserver: ${(it as RecyclerViewScrollLayout).scrollableView}")
                }
            }
            if (log) {
                Log.d(TAG, "notifyObservers end: ${observers.size}")
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

internal data class ScrollDataWrapper(
    var scrollState: ScrollState,
    var data: Any
) {
    var notifying: Boolean = false
}

internal object StopScroll