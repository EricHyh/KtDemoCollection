package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.InvokeWithParam
import com.hyh.list.FlatListItem
import com.hyh.list.IFlatListItem
import com.hyh.list.SimpleItemPagingSource
import com.hyh.list.ViewHolderFactory
import com.hyh.list.internal.SourceDisplayedData
import com.hyh.sticky.IStickyHeader
import kotlinx.coroutines.delay

/**
 * TODO: Add Description
 *
 * @author eriche 2022/6/21
 */
class TestNumItemPagingSource : SimpleItemPagingSource<Int>(0) {
    companion object {

        private const val TAG = "TestNumItemPagingSource"
    }


    private val stateMap: MutableMap<Int, Boolean> = mutableMapOf()

    private fun isExpand(group: Int): Boolean {
        return stateMap[group] != false
    }


    private val invoke = { group: Int ->
        stateMap[group] = !isExpand(group)
        rearrangeActuator(true)
    }


    val wholeItemsMap: MutableMap<Int, List<Int>> = mutableMapOf()

    var loadNum = 0

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FlatListItem> {
        Log.d(TAG, "load: ")
        //if (loadNum++ % 2 == 0) return LoadResult.Error(NullPointerException())
        when (params) {
            is LoadParams.Refresh -> {
                delay(2000)
                val items = mutableListOf<FlatListItem>()
                items.add(GroupTitleListItem(0, isExpand(0), invoke))
                if (isExpand(0)) {
                    for (index in 0..20) {
                        items.add(NumFlatListItem(params.param?.toString() ?: "", index, index))
                    }
                }
                val resultExtra = ResultExtra()
                resultExtra.wholeItemsMap[0] = (0..20).toList()
                return LoadResult.Success(items, 1, resultExtra = resultExtra)
            }
            is LoadParams.Append -> {
                delay(2000)
                val items = mutableListOf<FlatListItem>()
                val param = params.param ?: 1
                items.add(GroupTitleListItem(param, isExpand(param), invoke))
                if (isExpand(param)) {
                    for (index in 0..20) {
                        items.add(NumFlatListItem(param.toString(), index, index))
                    }
                }

                val resultExtra = displayedData?.resultExtra as? ResultExtra
                resultExtra?.wholeItemsMap?.put(param, (0..20).toList())

                return LoadResult.Success(items, param + 1, param == 20, resultExtra)
            }
            is LoadParams.Rearrange -> {
                val resultExtra = params.displayedData.resultExtra as? ResultExtra
                val items = mutableListOf<FlatListItem>()
                resultExtra?.wholeItemsMap?.forEach {
                    val expand = isExpand(it.key)
                    items.add(GroupTitleListItem(it.key, expand, invoke))
                    if (expand) {
                        it.value.forEach { num ->
                            items.add(NumFlatListItem(it.key.toString(), num, num))
                        }
                    }
                }
                Log.d(TAG, "load: ")
                return LoadResult.Rearranged(
                    ignore = false,
                    items,
                    resultExtra
                )
            }
        }
    }


    override fun onResultDisplayed(displayedData: SourceDisplayedData) {
        super.onResultDisplayed(displayedData)

    }

    override suspend fun getRefreshKey(): Int? {
        return null
    }

    override val sourceToken: Any
        get() = TestNumItemPagingSource::class.java


    private class ResultExtra {

        val wholeItemsMap: MutableMap<Int, List<Int>> = mutableMapOf()

    }

}


class GroupTitleListItem constructor(
    private val group: Int,
    private val expand: Boolean,
    private val clickInvoke: InvokeWithParam<Int>
) : IFlatListItem<RecyclerView.ViewHolder>(), IStickyHeader {

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
        return 100
    }

    override fun getViewHolderFactory(): ViewHolderFactory {
        return {
            //SystemClock.sleep(10)
            val textView = TextView(it.context)
            textView.setTextColor(Color.BLACK)
            textView.setBackgroundColor(Color.GRAY)
            textView.setPadding(20, 10, 0, 10)
            textView.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            textView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            object : RecyclerView.ViewHolder(textView) {}
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val expandStr = "点击${if (expand) "折叠" else "展开"}"
        (viewHolder.itemView as TextView).text = "第${group}组 - $expandStr"
        viewHolder.itemView.setOnClickListener {
            clickInvoke(group)
        }
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is GroupTitleListItem) return false
        return group == newItem.group
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is GroupTitleListItem) return false
        if (group != newItem.group) return false
        if (expand != newItem.expand) return false
        return true
    }
}

