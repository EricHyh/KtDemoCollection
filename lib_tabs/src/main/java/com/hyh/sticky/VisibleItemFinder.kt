package com.hyh.sticky

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * 列表可见Item查找器
 *
 * @author eriche
 * @data 2020/11/30
 */
interface VisibleItemFinder {

    fun findFirstVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int
    fun findFirstCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int

    fun findLastVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int
    fun findLastCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int
}

class DefaultVisibleItemFinder : VisibleItemFinder {

    var tempIntArray: IntArray? = null

    override fun findFirstVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
        if (layoutManager == null) return RecyclerView.NO_POSITION

        return when (layoutManager) {
            is LinearLayoutManager -> {
                layoutManager.findFirstVisibleItemPosition()
            }
            is StaggeredGridLayoutManager -> {
                val into = obtainIntArray(layoutManager.spanCount)
                layoutManager.findFirstVisibleItemPositions(into)
                findMin(into)
            }
            else -> RecyclerView.NO_POSITION
        }
    }

    override fun findFirstCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
        if (layoutManager == null) return RecyclerView.NO_POSITION

        return when (layoutManager) {
            is LinearLayoutManager -> {
                layoutManager.findFirstCompletelyVisibleItemPosition()
            }
            is StaggeredGridLayoutManager -> {
                val into = obtainIntArray(layoutManager.spanCount)
                layoutManager.findFirstCompletelyVisibleItemPositions(into)
                findMin(into)
            }
            else -> RecyclerView.NO_POSITION
        }
    }

    override fun findLastVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
        if (layoutManager == null) return RecyclerView.NO_POSITION

        return when (layoutManager) {
            is LinearLayoutManager -> {
                layoutManager.findLastVisibleItemPosition()
            }
            is StaggeredGridLayoutManager -> {
                val into = obtainIntArray(layoutManager.spanCount)
                layoutManager.findLastVisibleItemPositions(into)
                findMax(into)
            }
            else -> RecyclerView.NO_POSITION
        }
    }

    override fun findLastCompletelyVisibleItemPosition(layoutManager: RecyclerView.LayoutManager?): Int {
        if (layoutManager == null) return RecyclerView.NO_POSITION

        return when (layoutManager) {
            is LinearLayoutManager -> {
                layoutManager.findLastCompletelyVisibleItemPosition()
            }
            is StaggeredGridLayoutManager -> {
                val into = obtainIntArray(layoutManager.spanCount)
                layoutManager.findLastCompletelyVisibleItemPositions(into)
                findMax(into)
            }
            else -> RecyclerView.NO_POSITION
        }
    }

    private fun obtainIntArray(size: Int): IntArray {
        return if (size == tempIntArray?.size) {
            tempIntArray!!
        } else {
            tempIntArray = IntArray(size)
            tempIntArray!!
        }
    }

    private fun findMin(firstPositions: IntArray): Int {
        var min = firstPositions[0]
        for (value in firstPositions) {
            if (value == RecyclerView.NO_POSITION) continue
            if (value < min) {
                min = value
            }
        }
        return min
    }

    private fun findMax(lastPositions: IntArray): Int {
        var max = lastPositions[0]
        for (value in lastPositions) {
            if (value > max) {
                max = value
            }
        }
        return max
    }
}