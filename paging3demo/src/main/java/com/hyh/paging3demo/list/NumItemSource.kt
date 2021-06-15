package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.*

class NumItemSource(private val type: String) : SimpleItemSource<Unit>() {

    override suspend fun getPreShowJustFirstTime(param: Unit): IItemSource.PreShowResult {
        return IItemSource.PreShowResult.Unused
    }

    override suspend fun load(param: Unit): IItemSource.LoadResult {
        val items = mutableListOf<ItemData>()
        for (index in 0 until 6) {
            items.add(NumItemData(type, index))
        }
        return IItemSource.LoadResult.Success(items)
    }
}


class NumItemData(
    private val type: String,
    private val num: Int
) : IItemData<RecyclerView.ViewHolder> {

    override fun getItemViewType(): Int {
        return 0
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

    override fun areItemsTheSame(other: ItemData): Boolean {
        if(other !is NumItemData)return false
        return this.type == other.type && this.num == other.num
    }

    override fun areContentsTheSame(other: ItemData): Boolean {
        if(other !is NumItemData)return false
        return this.type == other.type && this.num == other.num
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        SystemClock.sleep(10)
        (viewHolder.itemView as TextView).text = "$type:$num"
    }
}