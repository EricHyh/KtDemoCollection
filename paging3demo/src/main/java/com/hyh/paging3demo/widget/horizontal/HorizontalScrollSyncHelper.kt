package com.hyh.paging3demo.widget.horizontal

import com.hyh.paging3demo.widget.horizontal.internal.IScrolledData
import com.hyh.paging3demo.widget.horizontal.internal.IScrollingData
import com.hyh.paging3demo.widget.horizontal.internal.OnScrollEventListener
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

    private var notifying: Boolean = false

    internal fun addObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.addObserver(observer)
        sync(observer)
    }

    internal fun removeObserver(observer: ScrollSyncObserver) {
        scrollSyncObservable.deleteObserver(observer)
        actionDownPublishers.remove(observer)
    }

    internal fun sync(observer: ScrollSyncObserver) {
        when (scrollDataWrapper.scrollState) {
            ScrollState.INITIAL -> {
                observer.dispatchInitial()
            }
            ScrollState.IDLE -> {
                observer.dispatchIdle(scrollDataWrapper.data as IScrolledData)
            }
            ScrollState.SCROLL -> {
                observer.dispatchScrolled(scrollDataWrapper.scrollState, scrollDataWrapper.data as IScrolledData)
            }
            ScrollState.DRAG -> {
                observer.dispatchDragging(scrollDataWrapper.data as Int)
            }
            ScrollState.REBOUND -> {
                observer.dispatchRebounding(scrollDataWrapper.data as Int)
            }
            ScrollState.SETTLING -> {
                observer.dispatchScrolled(scrollDataWrapper.scrollState, scrollDataWrapper.data as IScrolledData)
            }
        }
    }

    internal fun isAllowReleaseDrag(observer: ScrollSyncObserver): Boolean {
        if (actionDownPublishers.size <= 0) return true
        if (actionDownPublishers.size == 1 && actionDownPublishers.contains(observer)) return true
        return false
    }


    internal fun notifyDragging(currentDragWith: Int) {
        executeNotify {
            if (ScrollState.DRAG == scrollDataWrapper.scrollState && currentDragWith == scrollDataWrapper.data) return@executeNotify
            scrollDataWrapper.scrollState = ScrollState.DRAG
            scrollDataWrapper.data = currentDragWith
            scrollSyncObservable.notifyDragging(currentDragWith)
        }
    }

    internal fun notifyRebounding(currentDragWith: Int) {
        executeNotify {
            if (ScrollState.DRAG == scrollDataWrapper.scrollState && currentDragWith == scrollDataWrapper.data) return@executeNotify
            scrollDataWrapper.scrollState = ScrollState.REBOUND
            scrollDataWrapper.data = currentDragWith
            scrollSyncObservable.notifyRebounding(currentDragWith)
        }
    }

    internal fun notifyScrolling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
        executeNotify {
            scrollDataWrapper.scrollState = ScrollState.SCROLL
            val oldScrollData = scrollDataWrapper.data
            scrollDataWrapper.data = if (oldScrollData is IScrolledData && oldScrollData.copy(scrolledData)) {
                oldScrollData
            } else {
                scrolledData.clone()
            }
            scrollSyncObservable.notifyScrolling(scrollingData, scrolledData)
        }
    }

    internal fun notifySettling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
        executeNotify {
            scrollDataWrapper.scrollState = ScrollState.SETTLING
            val oldScrollData = scrollDataWrapper.data
            scrollDataWrapper.data = if (oldScrollData is IScrolledData && oldScrollData.copy(scrolledData)) {
                oldScrollData
            } else {
                scrolledData.clone()
            }
            scrollSyncObservable.notifySettling(scrollingData, scrolledData)
        }
    }

    internal fun notifyScrolled(scrollState: ScrollState, scrolledData: IScrolledData) {
        executeNotify {
            scrollDataWrapper.scrollState = scrollState
            val oldScrollData = scrollDataWrapper.data
            scrollDataWrapper.data = if (oldScrollData is IScrolledData && oldScrollData.copy(scrolledData)) {
                oldScrollData
            } else {
                scrolledData.clone()
            }
            scrollSyncObservable.notifyScrolled(scrollState, scrolledData)
        }
    }

    internal fun notifyIdle(scrolledData: IScrolledData) {
        executeNotify {
            scrollDataWrapper.scrollState = ScrollState.IDLE
            val oldScrollData = scrollDataWrapper.data
            scrollDataWrapper.data = if (oldScrollData is IScrolledData && oldScrollData.copy(scrolledData)) {
                oldScrollData
            } else {
                scrolledData.clone()
            }
            scrollSyncObservable.notifyIdle(scrolledData)
        }
    }

    internal fun notifyInitial() {
        executeNotify {
            scrollDataWrapper.scrollState = ScrollState.INITIAL
            scrollDataWrapper.data = Unit
            scrollSyncObservable.notifyInitial()
        }
    }

    internal fun notifyActionDown(publisher: ScrollSyncObserver) {
        actionDownPublishers.add(publisher)
        executeNotify {
            scrollSyncObservable.notifyStopScroll()
        }
    }

    internal fun notifyActionCancel(publisher: ScrollSyncObserver) {
        actionDownPublishers.remove(publisher)
    }

    private fun executeNotify(notify: () -> Unit) {
        if (notifying) return
        notifying = true
        notify.invoke()
        notifying = false
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

        fun notifyDragging(currentDragWith: Int) {
            observers.forEach {
                it.dispatchDragging(currentDragWith)
            }
        }

        fun notifyRebounding(currentDragWith: Int) {
            observers.forEach {
                it.dispatchRebounding(currentDragWith)
            }
        }

        fun notifyScrolling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
            observers.forEach {
                it.dispatchScrolling(scrollingData, scrolledData)
            }
        }

        fun notifySettling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
            observers.forEach {
                it.dispatchSettling(scrollingData, scrolledData)
            }
        }

        fun notifyScrolled(scrollState: ScrollState, scrolledData: IScrolledData) {
            observers.forEach {
                it.dispatchScrolled(scrollState, scrolledData)
            }
        }

        fun notifyIdle(scrolledData: IScrolledData) {
            observers.forEach {
                it.dispatchIdle(scrolledData)
            }
        }

        fun notifyInitial() {
            observers.forEach {
                it.dispatchInitial()
            }
        }

        fun notifyStopScroll() {
            observers.forEach {
                it.dispatchStopScroll()
            }
        }
    }
}

abstract class ScrollSyncObserver : OnScrollEventListener {

    private var dispatching: Boolean = false

    fun dispatchDragging(currentDragWith: Int) {
        executeDispatch {
            onDragging(currentDragWith)
        }
    }

    fun dispatchRebounding(currentDragWith: Int) {
        executeDispatch {
            onRebounding(currentDragWith)
        }
    }

    fun dispatchScrolling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
        executeDispatch {
            onScrolling(scrollingData, scrolledData)
        }
    }

    fun dispatchScrolled(scrollState: ScrollState, scrolledData: IScrolledData) {
        executeDispatch {
            onScrolled(scrollState, scrolledData)
        }
    }

    fun dispatchSettling(scrollingData: IScrollingData, scrolledData: IScrolledData) {
        executeDispatch {
            onSettling(scrollingData, scrolledData)
        }
    }

    fun dispatchIdle(scrolledData: IScrolledData) {
        executeDispatch {
            onIdle(scrolledData)
        }
    }

    fun dispatchInitial() {
        executeDispatch {
            onInitial()
        }
    }

    fun dispatchStopScroll() {
        executeDispatch {
            onStopScroll()
        }
    }

    abstract fun onScrolled(scrollState: ScrollState, scrolledData: IScrolledData)

    abstract fun onStopScroll()


    private fun executeDispatch(dispatch: () -> Unit) {
        if (dispatching) return
        dispatching = true
        dispatch.invoke()
        dispatching = false
    }

}

internal data class ScrollDataWrapper(
    var scrollState: ScrollState,
    var data: Any
)