package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.random.Random

class NumItemSource(private val type: String, override val sourceToken: Any = type) : SimpleItemSource<Unit>() {


    companion object {
        const val TAG = "NumItemSource"
    }


    private var lastNums: List<Int> = emptyList()

    override suspend fun getPreShow(param: Unit): PreShowResult<FlatListItem> {
        val titleItemData = TitleFlatListItem(type, lastNums, emptyList())
        return PreShowResult.Success(listOf(titleItemData))
    }

    override suspend fun load(param: Unit): LoadResult<FlatListItem> {
        delay(1000)
        val items = mutableListOf<FlatListItem>()
        val random = Random(SystemClock.currentThreadTimeMillis())
        val count = random.nextLong(5, 10).toInt()
        val nums = mutableListOf<Int>()
        for (index in 0 until count) {
            nums.add(index)
        }
        nums.sortBy {
            Math.random()
        }
        val titleItemData = TitleFlatListItem(type, lastNums, nums)
        lastNums = nums

        val numItems = nums.map { NumFlatListItem(type, it) }

        items.add(titleItemData)
        items.addAll(numItems)

        return LoadResult.Success(items)
    }

    override suspend fun getParam() = Unit

    override fun getFetchDispatcher(param: Unit): CoroutineDispatcher {
        return Dispatchers.IO
    }
}


class TitleFlatListItem(
    private val type: String,
    private val lastNums: List<Int>,
    private val curNums: List<Int>,
) : IFlatListItem<RecyclerView.ViewHolder>() {

    var activated = false

    override fun onItemActivated() {
        super.onItemActivated()
        ListConfig.aliveItems++
        //Log.d(NumItemSource.TAG, "${Thread.currentThread()} - TitleItemData.onActivated: ${ListConfig.aliveItems}")
        activated = true
        Log.d(NumItemSource.TAG, "${this}:onActivated: $activated")
    }

    override fun onItemDetached() {
        super.onItemDetached()
        ListConfig.aliveItems--
        //Log.d(NumItemSource.TAG, "${Thread.currentThread()} - TitleItemData.onDestroyed: ${ListConfig.aliveItems}")
        activated = false
        Log.d(NumItemSource.TAG, "${this}:onDestroyed: $activated")
    }


    override fun getItemViewType(): Int {
        return 0
    }

    override fun getViewHolderFactory(): ViewHolderFactory {
        return {
            //SystemClock.sleep(10)
            val textView = TextView(it.context)
            textView.setTextColor(Color.BLACK)
            textView.setBackgroundColor(Color.GRAY)
            textView.setPadding(20, 10, 0, 10)
            textView.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            object : RecyclerView.ViewHolder(textView) {}
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        var lastNumsStr = "上一次序列："
        var curNumsStr = "这一次序列："
        lastNums.let { nums ->
            nums.forEach {
                lastNumsStr += it.toString()
            }
        }
        curNums.let { nums ->
            nums.forEach {
                curNumsStr += it.toString()
            }
        }
        (viewHolder.itemView as TextView).text = "$type:\n\t$lastNumsStr\n\t$curNumsStr"
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is TitleFlatListItem) return false
        return type == newItem.type
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is TitleFlatListItem) return false
        return false
    }
}

class NumFlatListItem(
    private val type: String,
    private val num: Int
) : IFlatListItem<RecyclerView.ViewHolder>() {

    var activated = false

    override fun onItemActivated() {
        super.onItemActivated()
        ListConfig.aliveItems++
        //Log.d(NumItemSource.TAG, "${Thread.currentThread()} - NumItemData onActivated: ${ListConfig.aliveItems}")
        activated = true
        Log.d(NumItemSource.TAG, "${this}:onActivated: $activated")
    }

    override fun onItemDetached() {
        super.onItemDetached()
        ListConfig.aliveItems--
        //Log.d(NumItemSource.TAG, "${Thread.currentThread()} - NumItemData onDestroyed: ${ListConfig.aliveItems}")
        activated = false
        Log.d(NumItemSource.TAG, "${this}:onDestroyed: $activated")
    }

    override fun getItemViewType(): Int {
        return 1
    }

    override fun getViewHolderFactory(): ViewHolderFactory {
        return {
            //SystemClock.sleep(10)
            val textView = TextView(it.context)
            textView.setTextColor(Color.BLACK)
            textView.setBackgroundColor(Color.WHITE)
            textView.gravity = Gravity.CENTER
            textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
            object : RecyclerView.ViewHolder(textView) {}
        }
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is NumFlatListItem) return false
        return this.type == newItem.type && this.num == newItem.num
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is NumFlatListItem) return false
        return this.type == newItem.type && this.num == newItem.num
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        SystemClock.sleep(10)
        (viewHolder.itemView as TextView).text = "$type:$num"
    }
}