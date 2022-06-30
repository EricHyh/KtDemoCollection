package com.hyh.paging3demo.list

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.base.RefreshStrategy
import com.hyh.list.*
import com.hyh.list.internal.ListItemWrapper
import com.hyh.list.internal.SourceDisplayedData
import com.hyh.paging3demo.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

class TestMultiContentItemSource(override val sourceToken: Any) : MultiContentItemSource<Int>() {

    companion object {
        private const val TAG = "TestMultiTabsItemSource"
    }

    private var selectedTab: Int = 0

    private val onTabClick: (tab: Int) -> Unit = {
        if (selectedTab != it) {
            selectedTab = it
            refreshActuator.invoke(true)
        }
    }

    override fun getRefreshStrategy(): RefreshStrategy {
        return RefreshStrategy.DelayedQueueUp(5000)
    }

    override suspend fun getTitlePreShow(tabToken: Any, param: Int): List<FlatListItem> {
        return listOf(MultiTabsTitleFlatListItem(param, onTabClick))
    }

    override suspend fun getContentPreShow(tabToken: Any, param: Int): List<FlatListItem> {
        return emptyList()
    }

    override suspend fun getContent(tabToken: Any, param: Int): ContentResult {
        Log.d(TAG, "getContent delay1: $tabToken ${Thread.currentThread()}")
        //delay(2000)
        Log.d(TAG, "getContent delay2: $tabToken ${Thread.currentThread()}")
        when (param) {
            0 -> {
                val list = mutableListOf<Tab1FlatListItem>()
                for (index in 0..4) {
                    list.add(Tab1FlatListItem(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
            1 -> {
                /*val list = mutableListOf<Tab2ItemData>()
                for (index in 0..9) {
                    list.add(Tab2ItemData("条目: $index", "这是条目: $index"))
                }*/
                val list = mutableListOf<Tab1FlatListItem>()
                for (index in 0..7) {
                    list.add(Tab1FlatListItem(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
            2 -> {
                val list = mutableListOf<Tab3FlatListItem>()
                for (index in 0..19) {
                    list.add(Tab3FlatListItem(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
        }
        return ContentResult.Success(emptyList())
    }

    override fun getContentTokenFromParam(param: Int): Any {
        return param
    }

    override suspend fun getParam(): Int {
        return selectedTab
    }

    override fun getFetchDispatcher(param: Int, displayedData: SourceDisplayedData): CoroutineDispatcher {
        return Dispatchers.IO
    }

    private fun getRandomColor(): Int {
        val colorIntList = listOf(
            Color.WHITE, Color.GRAY, Color.BLACK, Color.RED, Color.BLUE, Color.CYAN, Color.LTGRAY, Color.YELLOW, Color.MAGENTA
        )
        val random = Random(System.currentTimeMillis())
        return colorIntList[random.nextInt(0, colorIntList.size)]
    }

    override fun isEmptyContent(items: List<FlatListItem>): Boolean {
        return false
    }
}

class MultiTabsTitleFlatListItem(
    private var selectedTab: Int,
    private val onTabClick: (tab: Int) -> Unit
) : IFlatListItem<MultiTabsTitleFlatListItem.TitleHolder>() {

    override fun getItemViewType(): Int {
        return 0
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<TitleHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_title, it, false)
            TitleHolder(view)
        }
    }


    override fun onBindViewHolder(
        viewHolder: TitleHolder
    ) {
        viewHolder.tvTab1.text = "Tab1(${if (selectedTab == 0) "选中" else "未选中"})"
        viewHolder.tvTab1.setOnClickListener {
            onTabClick(0)
        }
        viewHolder.tvTab2.text = "Tab2(${if (selectedTab == 1) "选中" else "未选中"})"
        viewHolder.tvTab2.setOnClickListener {
            onTabClick(1)
        }
        viewHolder.tvTab3.text = "Tab3(${if (selectedTab == 2) "选中" else "未选中"})"
        viewHolder.tvTab3.setOnClickListener {
            onTabClick(2)
        }
    }

    override fun isSupportUpdateItem(): Boolean {
        return false
    }

    override fun onUpdateItem(newItem: FlatListItem) {
        super.onUpdateItem(newItem)
        selectedTab = (newItem as MultiTabsTitleFlatListItem).selectedTab
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is MultiTabsTitleFlatListItem) return false
        return true
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return false
        /*if (newItemData !is MultiTabsTitleItemData) return false
        return this.selectedTab == newItemData.selectedTab*/
    }

    override fun getChangePayload(newItem: FlatListItem): Any? {
        return (newItem as MultiTabsTitleFlatListItem).selectedTab
    }

    class TitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTab1: TextView = itemView.findViewById(R.id.tv_tab1)
        val tvTab2: TextView = itemView.findViewById(R.id.tv_tab2)
        val tvTab3: TextView = itemView.findViewById(R.id.tv_tab3)
    }
}

class Tab1FlatListItem(
    private val leftViewColorInt: Int,
    private val title: String,
    private val des: String
) : IFlatListItem<Tab1FlatListItem.Tab1ItemHolder>() {

    override fun getItemViewType(): Int {
        return 1
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<Tab1ItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_tab1, it, false)
            Tab1ItemHolder(view)
        }
    }

    override fun onBindViewHolder(viewHolder: Tab1ItemHolder) {
        viewHolder.leftView.setBackgroundColor(leftViewColorInt)
        viewHolder.tvTitle.text = title
        viewHolder.tvDes.text = des
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is Tab1FlatListItem) return false
        return this.leftViewColorInt == newItem.leftViewColorInt
                && this.title == newItem.title
                && this.des == newItem.des
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return false
    }

    class Tab1ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leftView: View = itemView.findViewById(R.id.left_view)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDes: TextView = itemView.findViewById(R.id.tv_des)
    }
}

class Tab2FlatListItem(
    private val title: String,
    private val des: String
) : IFlatListItem<Tab2FlatListItem.Tab2ItemHolder>() {

    override fun getItemViewType(): Int {
        return 2
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<Tab2ItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_tab2, it, false)
            Tab2ItemHolder(view)
        }
    }

    override fun onBindViewHolder(viewHolder: Tab2ItemHolder) {
        viewHolder.tvTitle.text = title
        viewHolder.tvDes.text = des
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is Tab2FlatListItem) return false
        return this.title == newItem.title
                && this.des == newItem.des
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return false
    }

    class Tab2ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDes: TextView = itemView.findViewById(R.id.tv_des)
    }
}


class Tab3FlatListItem(
    private val rightViewColorInt: Int,
    private val title: String,
    private val des: String
) : IFlatListItem<Tab3FlatListItem.Tab3ItemHolder>() {

    override fun getItemViewType(): Int {
        return 3
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<Tab3ItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_tab3, it, false)
            Tab3ItemHolder(view)
        }
    }

    override fun onBindViewHolder(viewHolder: Tab3ItemHolder) {
        viewHolder.rightView.setBackgroundColor(rightViewColorInt)
        viewHolder.tvTitle.text = title
        viewHolder.tvDes.text = des
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is Tab3FlatListItem) return false
        return this.rightViewColorInt == newItem.rightViewColorInt
                && this.title == newItem.title
                && this.des == newItem.des
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return false
    }

    class Tab3ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rightView: View = itemView.findViewById(R.id.right_view)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDes: TextView = itemView.findViewById(R.id.tv_des)
    }
}