package com.hyh.paging3demo.widget.horizontal.internal

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewScrollable(private val recyclerView: RecyclerView) :
    Scrollable<RecyclerViewScrollable.RecyclerViewScrollData> {

    companion object {
        private const val TAG = "RecyclerViewScrollable"
    }

    private val linearLayoutManager: LinearLayoutManager =
        recyclerView.layoutManager as LinearLayoutManager

    private val recyclerViewScrollData: RecyclerViewScrollData = RecyclerViewScrollData()

    override fun getScrollData(): RecyclerViewScrollData {
        val position = linearLayoutManager.findFirstVisibleItemPosition()
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        return recyclerViewScrollData.apply {
            this.position = position
            this.positionOffset = holder?.itemView?.left
            //this.globalOffset = if (position >= 0 && holder?.itemView?.left != null) -1 else recyclerView.computeHorizontalScrollOffset()
            this.globalOffset = recyclerView.computeHorizontalScrollOffset()
        }
    }

    override fun scrollTo(t: RecyclerViewScrollData) {
        val newPosition = t.position
        val newPositionOffset = t.positionOffset



        if (newPosition < 0 || newPositionOffset == null) {
            recyclerView.scrollBy(t.globalOffset - recyclerView.computeHorizontalScrollOffset(), 0)
            return
        }

        val firstPosition = linearLayoutManager.findFirstVisibleItemPosition()
        val lastPosition = linearLayoutManager.findLastVisibleItemPosition()

        val visibleItemCount = (lastPosition - firstPosition) + 1

        if (recyclerView.childCount <= 0
            || visibleItemCount <= 0
            || recyclerView.childCount > visibleItemCount
        ) {
            Log.d(TAG, "scrollToPositionWithOffset1: ")
            Log.d(TAG, "scrollToPositionWithOffset1: ")
            linearLayoutManager.scrollToPositionWithOffset(newPosition, newPositionOffset)
            return
        }

        if (firstPosition != newPosition /*&& newPosition > firstPosition*/) {
            Log.d(TAG, "scrollToPositionWithOffset2: $firstPosition $newPosition $newPositionOffset")
            linearLayoutManager.scrollToPositionWithOffset(newPosition, newPositionOffset)
            return
        }


        var needRequestLayout = false
        var right: Int? = null
        kotlin.run {

            for (index in 0 until recyclerView.childCount){
                val view = recyclerView.getChildAt(index)
                val viewHolder = recyclerView.findContainingViewHolder(view)
                if (viewHolder == null) {
                    needRequestLayout = true
                    Log.d(TAG, "scrollTo: ")
                    return@run
                }
                if (right != null) {
                    if (right != viewHolder.itemView.left) {
                        needRequestLayout = true
                        Log.d(TAG, "scrollTo: $index ${viewHolder.absoluteAdapterPosition}")
                        return@run
                    }
                    right = viewHolder.itemView.right
                }
            }


            for (index in firstPosition..lastPosition) {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(index)
                if (viewHolder == null) {
                    needRequestLayout = true
                    Log.d(TAG, "scrollTo: ")
                    return@run
                }
                if (right != null) {
                    if (right != viewHolder.itemView.left) {
                        needRequestLayout = true
                        Log.d(TAG, "scrollTo: ")
                        return@run
                    }
                    right = viewHolder.itemView.right
                }
            }
        }

        if (needRequestLayout) {
            Log.d(TAG, "scrollToPositionWithOffset3: $firstPosition $newPosition $newPositionOffset")
            linearLayoutManager.scrollToPositionWithOffset(newPosition, newPositionOffset)
            return
        }

        Log.d(TAG, "scrollBy: ")
        val dx = t.globalOffset - recyclerView.computeHorizontalScrollOffset()
        if (dx == 0) return
        recyclerView.scrollBy(dx, 0)
    }

    override fun resetScroll() {
        linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    data class RecyclerViewScrollData(
        var position: Int = -1,
        var positionOffset: Int? = null,
        var globalOffset: Int = -1
    ) : IScrollData {
        override fun toString(): String {
            return "RecyclerViewScrollData(position=$position, positionOffset=$positionOffset, globalOffset=$globalOffset)"
        }

        override fun clone(): Any {
            return RecyclerViewScrollData(position, positionOffset, globalOffset)
        }

        override fun copy(other: IScrollData) {
            if (other !is RecyclerViewScrollData) return
            this.position = other.position
            this.positionOffset = other.positionOffset
            this.globalOffset = other.globalOffset
        }
    }

    override fun stopScroll() {
        recyclerView.stopScroll()
    }
}