package com.hyh.paging3demo.list

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.base.LoadStrategy
import com.hyh.list.*
import com.hyh.paging3demo.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.random.Random

class TestMultiTabsItemSource(override val sourceToken: Any) : MultiTabsItemSource<Int>() {

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

    /*override fun getLoadStrategy(): LoadStrategy {
        return LoadStrategy.DelayedQueueUp(5000)
    }*/

    override suspend fun getTitlePreShow(tabToken: Any, param: Int): List<ItemData> {
        return listOf(MultiTabsTitleItemData(param, onTabClick))
    }

    override suspend fun getContentPreShow(tabToken: Any, param: Int): List<ItemData> {
        return emptyList()
    }

    override suspend fun getContent(tabToken: Any, param: Int): ContentResult {
        Log.d(TAG, "getContent delay1: $tabToken ${Thread.currentThread()}")
        //delay(2000)
        Log.d(TAG, "getContent delay2: $tabToken ${Thread.currentThread()}")
        when (param) {
            0 -> {
                val list = mutableListOf<Tab1ItemData>()
                for (index in 0..4) {
                    list.add(Tab1ItemData(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
            1 -> {
                /*val list = mutableListOf<Tab2ItemData>()
                for (index in 0..9) {
                    list.add(Tab2ItemData("条目: $index", "这是条目: $index"))
                }*/
                val list = mutableListOf<Tab1ItemData>()
                for (index in 0..7) {
                    list.add(Tab1ItemData(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
            2 -> {
                val list = mutableListOf<Tab3ItemData>()
                for (index in 0..19) {
                    list.add(Tab3ItemData(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
        }
        return ContentResult.Success(emptyList())
    }

    override fun getTabTokenFromParam(param: Int): Any {
        return param
    }

    override suspend fun getParam(): Int {
        return selectedTab
    }

    override fun getFetchDispatcher(param: Int): CoroutineDispatcher {
        return Dispatchers.IO
    }

    private fun getRandomColor(): Int {
        val colorIntList = listOf(
            Color.WHITE, Color.GRAY, Color.BLACK, Color.RED, Color.BLUE, Color.CYAN, Color.LTGRAY, Color.YELLOW, Color.MAGENTA
        )
        val random = Random(System.currentTimeMillis())
        return colorIntList[random.nextInt(0, colorIntList.size)]
    }
}

class MultiTabsTitleItemData(
    private var selectedTab: Int,
    private val onTabClick: (tab: Int) -> Unit
) : IItemData<MultiTabsTitleItemData.TitleHolder>() {

    override fun getItemViewType(): Int {
        return 0
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<TitleHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_title, it, false)
            TitleHolder(view)
        }
    }


    override fun onBindViewHolder(viewHolder: TitleHolder) {
        viewHolder.tvTab1.text = "Tab1(${if (selectedTab == 0) "选中" else "未选中"})"
        viewHolder.tvTab1.setOnClickListener {
            onTabClick(0)
        }
        viewHolder.tvTab2.text = "Tab1(${if (selectedTab == 1) "选中" else "未选中"})"
        viewHolder.tvTab2.setOnClickListener {
            onTabClick(1)
        }
        viewHolder.tvTab3.text = "Tab1(${if (selectedTab == 2) "选中" else "未选中"})"
        viewHolder.tvTab3.setOnClickListener {
            onTabClick(2)
        }
    }

    override fun isSupportUpdateItemData(): Boolean {
        return false
    }

    override fun onUpdateItemData(newItemData: ItemData) {
        super.onUpdateItemData(newItemData)
        selectedTab = (newItemData as MultiTabsTitleItemData).selectedTab
    }

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is MultiTabsTitleItemData) return false
        return true
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        return false
        /*if (newItemData !is MultiTabsTitleItemData) return false
        return this.selectedTab == newItemData.selectedTab*/
    }

    override fun getChangePayload(newItemData: ItemData): Any? {
        return (newItemData as MultiTabsTitleItemData).selectedTab
    }

    class TitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTab1: TextView = itemView.findViewById(R.id.tv_tab1)
        val tvTab2: TextView = itemView.findViewById(R.id.tv_tab2)
        val tvTab3: TextView = itemView.findViewById(R.id.tv_tab3)
    }
}

class Tab1ItemData(
    private val leftViewColorInt: Int,
    private val title: String,
    private val des: String
) : IItemData<Tab1ItemData.Tab1ItemHolder>() {

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

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is Tab1ItemData) return false
        return this.leftViewColorInt == newItemData.leftViewColorInt
                && this.title == newItemData.title
                && this.des == newItemData.des
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        return false
    }

    class Tab1ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leftView: View = itemView.findViewById(R.id.left_view)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDes: TextView = itemView.findViewById(R.id.tv_des)
    }
}

class Tab2ItemData(
    private val title: String,
    private val des: String
) : IItemData<Tab2ItemData.Tab2ItemHolder>() {

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

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is Tab2ItemData) return false
        return this.title == newItemData.title
                && this.des == newItemData.des
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        return false
    }

    class Tab2ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDes: TextView = itemView.findViewById(R.id.tv_des)
    }
}


class Tab3ItemData(
    private val rightViewColorInt: Int,
    private val title: String,
    private val des: String
) : IItemData<Tab3ItemData.Tab3ItemHolder>() {

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

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is Tab3ItemData) return false
        return this.rightViewColorInt == newItemData.rightViewColorInt
                && this.title == newItemData.title
                && this.des == newItemData.des
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        return false
    }

    class Tab3ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rightView: View = itemView.findViewById(R.id.right_view)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDes: TextView = itemView.findViewById(R.id.tv_des)
    }
}