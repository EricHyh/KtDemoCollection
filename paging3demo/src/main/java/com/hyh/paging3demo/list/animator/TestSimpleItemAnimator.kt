package com.hyh.paging3demo.list.animator

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/**
 * TODO: Add Description
 *
 * @author eriche 2023/2/9
 */
class TestSimpleItemAnimator : SimpleItemAnimator() {
    companion object {
        private const val TAG = "TestSimpleItemAnimator"
    }

    override fun runPendingAnimations() {
        Log.d(TAG, "runPendingAnimations: ")
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        fromLeft: Int,
        fromTop: Int,
        toLeft: Int,
        toTop: Int
    ): Boolean {
        Log.d(TAG, "animateChange: $oldHolder, $newHolder, $fromLeft, $fromTop, $toLeft, $fromLeft, $toTop")
        return false
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        Log.d(TAG, "animateRemove: $holder")
        return false
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        Log.d(TAG, "animateAdd: $holder")
        return false
    }

    override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        Log.d(TAG, "animateMove: $holder, $fromX, $fromY, $toX, $toY")
        return false
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        Log.d(TAG, "endAnimation: $item")
    }

    override fun endAnimations() {
        Log.d(TAG, "endAnimations")
    }

    override fun isRunning(): Boolean {
        Log.d(TAG, "isRunning")
        return false
    }
}