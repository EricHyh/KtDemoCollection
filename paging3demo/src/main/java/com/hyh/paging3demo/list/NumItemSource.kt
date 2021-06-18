package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.*
import kotlin.random.Random

class NumItemSource(private val type: String) : SimpleItemSource<Unit>() {

    private var lastNums: List<Int> = emptyList()

    override suspend fun getPreShowWhenTheFirstTime(param: Unit): ItemSource.PreShowResult {
        return ItemSource.PreShowResult.Unused
    }

    override suspend fun load(param: Unit): ItemSource.LoadResult {
        val items = mutableListOf<ItemData>()
        val random = Random(SystemClock.currentThreadTimeMillis())
        val count = random.nextLong(5, 10).toInt()
        val nums = mutableListOf<Int>()
        for (index in 0 until count) {
            nums.add(index)
        }
        nums.sortBy {
            Math.random()
        }
        val titleItemData = TitleItemData(type, lastNums, nums)
        lastNums = nums

        val numItems = nums.map { NumItemData(type, it) }

        items.add(titleItemData)
        items.addAll(numItems)

        return ItemSource.LoadResult.Success(items)
    }

    /*override fun getFetchDispatcher(param: Unit): CoroutineDispatcher {
        return Dispatchers.IO
    }*/
}


class TitleItemData(
    private val type: String,
    private val lastNums: List<Int>,
    private val curNums: List<Int>,
) : IItemData<RecyclerView.ViewHolder> {

    override fun getItemViewType(): Int {
        return 0
    }

    override fun getViewHolderFactory(): ViewHolderFactory {
        return {
            SystemClock.sleep(10)
            val textView = TextView(it.context)
            textView.setTextColor(Color.BLACK)
            textView.setBackgroundColor(Color.GRAY)
            textView.setPadding(20, 10, 0, 10)
            textView.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            object : RecyclerView.ViewHolder(textView) {}
        }
    }

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

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is TitleItemData) return false
        return type == newItemData.type
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        return false
    }
}

class NumItemData(
    private val type: String,
    private val num: Int
) : IItemData<RecyclerView.ViewHolder> {

    override fun getItemViewType(): Int {
        return 1
    }

    override fun getViewHolderFactory(): ViewHolderFactory {
        return {
            SystemClock.sleep(10)
            val textView = TextView(it.context)
            textView.setTextColor(Color.BLACK)
            textView.setBackgroundColor(Color.WHITE)
            textView.gravity = Gravity.CENTER
            textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
            object : RecyclerView.ViewHolder(textView) {}
        }
    }

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is NumItemData) return false
        return this.type == newItemData.type && this.num == newItemData.num
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is NumItemData) return false
        return this.type == newItemData.type && this.num == newItemData.num
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        SystemClock.sleep(10)
        (viewHolder.itemView as TextView).text = "$type:$num"
    }
}