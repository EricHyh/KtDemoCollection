package com.hyh.paging3demo.list

import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/6/16
 */
class TestAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "TestAdapter"
    }

    private var dataList: List<String> = mutableListOf()

    fun refresh() {
        val randomTypes = ListConfig.randomTypes()
        val newDataList = mutableListOf<String>()
        randomTypes.forEach {
            for (num in 0..10) {
                newDataList.add("$it:$num")
            }
        }
        val diffResult = DiffUtil.calculateDiff(DiffCallback(dataList, newDataList))
        dataList = newDataList
        diffResult.dispatchUpdatesTo(this)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        SystemClock.sleep(10)
        val textView = TextView(parent.context)
        textView.setTextColor(Color.BLACK)
        textView.setBackgroundColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
        return object : RecyclerView.ViewHolder(textView) {}
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        SystemClock.sleep(10)
        val data = dataList[position]
        (holder.itemView as TextView).text = data
    }

    class DiffCallback(private val oldDataList: List<String>, private val newDataList: List<String>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldDataList.size
        }

        override fun getNewListSize(): Int {
            return newDataList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldDataList[oldItemPosition] == newDataList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldDataList[oldItemPosition] == newDataList[newItemPosition]
        }
    }
}