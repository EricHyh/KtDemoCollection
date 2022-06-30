package com.hyh.paging3demo.list.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.RefreshActuator
import com.hyh.base.RefreshStrategy
import com.hyh.list.*
import com.hyh.list.adapter.MultiItemSourceAdapter
import com.hyh.list.decoration.RoundCorner
import com.hyh.list.decoration.RoundCornerBuilder
import com.hyh.list.internal.ListItemWrapper
import com.hyh.list.internal.SourceDisplayedData
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.random.Random

class AccountPageFragment : Fragment() {

    companion object {
        private const val TAG = "AccountPageFragment"

        var withItemAnimator = false
    }


    private val multiItemSourceAdapter: MultiItemSourceAdapter<Unit> by lazy {
        MultiItemSourceAdapter<Unit>(pageContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trade_tab_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_refresh).setOnClickListener {
            multiItemSourceAdapter.refreshRepo(Unit)
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (!withItemAnimator) {
            recyclerView.itemAnimator = null
        }

        val corners = RoundCornerBuilder()
            .topLeft(40F, RoundCorner.DIRECTION_IN)
            .topRight(40F, RoundCorner.DIRECTION_IN)
            .bottomLeft(40F, RoundCorner.DIRECTION_IN)
            .bottomRight(40F, RoundCorner.DIRECTION_IN)
            .build()


        recyclerView.adapter = multiItemSourceAdapter
        //sourceRepoAdapter.submitData(TradeTabItemSourceRepo().flow)
        multiItemSourceAdapter.submitData(
            MultiItemSourceRepo(
                listOf(AccountItemSource(true), AccountItemSource(false))
            ).flow
        )
    }
}


class AccountItemSource(private val testEmpty: Boolean) : MultiContentItemSource<Int>() {

    override val sourceToken: Any = testEmpty

    private val random = Random(System.currentTimeMillis())

    private var selectedTab: Int = 0

    private val onTabClick: (tab: Int) -> Unit = {
        if (selectedTab != it) {
            selectedTab = it
            refreshActuator.invoke(true)
        }
    }

    override fun getRefreshStrategy(): RefreshStrategy {
        //return RefreshStrategy.DelayedQueueUp(5000)
        return RefreshStrategy.CancelLast
    }


    override fun isEmptyContent(items: List<FlatListItem>): Boolean {
        if (items.isEmpty()) return true
        if (items.size == 1 && items[0] is EmptyFlatListItem) {
            return true
        }
        return false
    }

    override suspend fun getTitlePreShow(tabToken: Any, param: Int): List<FlatListItem> {
        return listOf(MultiTabsTitleFlatListItem(param, onTabClick))
    }

    override suspend fun getContentPreShow(tabToken: Any, param: Int): List<FlatListItem> {
        return if (testEmpty) {
            listOf(LoadingFlatListItem())
        } else {
            emptyList()
        }
    }

    override suspend fun getContent(tabToken: Any, param: Int): ContentResult {
        if (testEmpty) {
            delay(2000)
        }
        when (param) {
            0 -> {
                if (testEmpty) {
                    return ContentResult.Success(listOf(EmptyFlatListItem(refreshActuator)))
                }
                val list = mutableListOf<Tab1FlatListItem>()
                for (index in 0..4) {
                    list.add(Tab1FlatListItem(getRandomColor(), "条目: $index", "这是条目: $index"))
                }
                return ContentResult.Success(list)
            }
            1 -> {
                val list = mutableListOf<Tab2FlatListItem>()
                for (index in 0..9) {
                    list.add(Tab2FlatListItem("条目: $index", "这是条目: $index"))
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

    override fun getFetchDispatcher(
        param: Int,
        displayedData: SourceDisplayedData
    ): CoroutineDispatcher {
        return Dispatchers.IO
    }

    private fun getRandomColor(): Int {
        val colorIntList = listOf(
            Color.WHITE,
            Color.GRAY,
            Color.BLACK,
            Color.RED,
            Color.BLUE,
            Color.CYAN,
            Color.LTGRAY,
            Color.YELLOW,
            Color.MAGENTA
        )
        return colorIntList[random.nextInt(0, colorIntList.size)]
    }

}


class MultiTabsTitleFlatListItem(
    private var selectedTab: Int,
    private val onTabClick: (tab: Int) -> Unit
) : IFlatListItem<MultiTabsTitleFlatListItem.TitleHolder>() {

    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d("FlatListItem", "MultiTabsTitleFlatListItem onStateChanged: $event")
            }
        })
    }


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


class LoadingFlatListItem() : IFlatListItem<LoadingFlatListItem.LoadingItemHolder>() {

    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d("FlatListItem", "LoadingFlatListItem onStateChanged: $event")
            }
        })
    }

    override fun getItemViewType(): Int {
        return R.layout.item_loading
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<LoadingItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_loading, it, false)
            LoadingItemHolder(view)
        }
    }

    override fun onBindViewHolder(
        viewHolder: LoadingItemHolder
    ) {
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is LoadingFlatListItem) return false
        return true
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return true
    }


    class LoadingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
    }
}


class EmptyFlatListItem(val refresh: RefreshActuator) :
    IFlatListItem<EmptyFlatListItem.EmptyItemHolder>() {

    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d("FlatListItem", "EmptyFlatListItem onStateChanged: $event")
            }
        })
    }

    override fun getItemViewType(): Int {
        return R.layout.item_empty
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<EmptyItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_empty, it, false)
            EmptyItemHolder(view)
        }
    }

    override fun onBindViewHolder(
        viewHolder: EmptyItemHolder
    ) {
        viewHolder.btnRefresh.setOnClickListener {
            refresh.invoke(true)
        }
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is EmptyFlatListItem) return false
        if (refresh != newItem.refresh) return false
        return true
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return true
    }

    class EmptyItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnRefresh: Button = itemView.findViewById(R.id.btn_refresh)
    }
}


class Tab1FlatListItem(
    private val leftViewColorInt: Int,
    private val title: String,
    private val des: String
) : IFlatListItem<Tab1FlatListItem.Tab1ItemHolder>() {

    private val TAG = "Tab1FlatListItem"

    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "Tab1FlatListItem onStateChanged: $event - $this")
            }
        })
    }

    override fun getItemViewType(): Int {
        return 1
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<Tab1ItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_tab1, it, false)
            Tab1ItemHolder(view)
        }
    }

    override fun onBindViewHolder(
        viewHolder: Tab1ItemHolder
    ) {
        viewHolder.leftView.setBackgroundColor(leftViewColorInt)
        viewHolder.tvTitle.text = title
        viewHolder.tvDes.text = des

        Log.d(TAG, "onBindViewHolder: $viewHolder")
    }

    override fun onViewAttachedToWindow(viewHolder: Tab1ItemHolder) {
        Log.d(TAG, "onViewAttachedToWindow: $viewHolder - $this")
        super.onViewAttachedToWindow(viewHolder)
    }

    override fun onViewDetachedFromWindow(viewHolder: Tab1ItemHolder) {
        super.onViewDetachedFromWindow(viewHolder)
        Log.d(TAG, "onViewDetachedFromWindow: $viewHolder - $this")
    }

    /*override fun onViewRecycled(viewHolder: Tab1ItemHolder) {
        super.onViewRecycled(viewHolder)
        Log.d(TAG, "onViewRecycled: $viewHolder")
    }

    override fun onFailedToRecycleView(viewHolder: Tab1ItemHolder): Boolean {
        Log.d(TAG, "onFailedToRecycleView: $viewHolder")
        return super.onFailedToRecycleView(viewHolder)
    }*/

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

    private val TAG = "Tab2FlatListItem"

    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "Tab2FlatListItem onStateChanged: $event - $this")
            }
        })
    }

    override fun getItemViewType(): Int {
        return 2
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<Tab2ItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_tab2, it, false)
            Tab2ItemHolder(view)
        }
    }

    override fun onBindViewHolder(
        viewHolder: Tab2ItemHolder
    ) {
        viewHolder.tvTitle.text = title
        viewHolder.tvDes.text = des

        Log.d(TAG, "onBindViewHolder: $viewHolder")
    }


    override fun onViewAttachedToWindow(viewHolder: Tab2ItemHolder) {
        Log.d(TAG, "onViewAttachedToWindow: $viewHolder - $this")
        super.onViewAttachedToWindow(viewHolder)
    }

    override fun onViewDetachedFromWindow(viewHolder: Tab2ItemHolder) {
        super.onViewDetachedFromWindow(viewHolder)
        Log.d(TAG, "onViewDetachedFromWindow: $viewHolder - $this")
    }

    /*override fun onViewRecycled(viewHolder: Tab2ItemHolder) {
        super.onViewRecycled(viewHolder)
        Log.d(TAG, "onViewRecycled: $viewHolder")
    }

    override fun onFailedToRecycleView(viewHolder: Tab2ItemHolder): Boolean {
        Log.d(TAG, "onFailedToRecycleView: $viewHolder")
        return super.onFailedToRecycleView(viewHolder)
    }*/

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is Tab2FlatListItem) return false
        return this.title == newItem.title
                && this.des == newItem.des
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return true
    }

    override fun onItemDetached() {
        super.onItemDetached()
        Log.d(TAG, "onItemDetached: ")
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

    private val TAG = "Tab3FlatListItem"

    init {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d(TAG, "Tab3FlatListItem onStateChanged: $event - $this")
            }
        })
    }

    override fun getItemViewType(): Int {
        return 3
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<Tab3ItemHolder> {
        return {
            val view = LayoutInflater.from(it.context).inflate(R.layout.item_tab3, it, false)
            Tab3ItemHolder(view)
        }
    }

    override fun onBindViewHolder(
        viewHolder: Tab3ItemHolder
    ) {
        viewHolder.rightView.setBackgroundColor(rightViewColorInt)
        viewHolder.tvTitle.text = title
        viewHolder.tvDes.text = des

        Log.d(TAG, "onBindViewHolder: $viewHolder")
    }

    override fun onViewAttachedToWindow(viewHolder: Tab3ItemHolder) {
        Log.d(TAG, "onViewAttachedToWindow: $viewHolder - $this")
        super.onViewAttachedToWindow(viewHolder)
    }

    override fun onViewDetachedFromWindow(viewHolder: Tab3ItemHolder) {
        super.onViewDetachedFromWindow(viewHolder)
        Log.d(TAG, "onViewDetachedFromWindow: $viewHolder - $this")
    }

    /*override fun onViewRecycled(viewHolder: Tab3ItemHolder) {
        super.onViewRecycled(viewHolder)
        Log.d(TAG, "onViewRecycled: $viewHolder")
    }

    override fun onFailedToRecycleView(viewHolder: Tab3ItemHolder): Boolean {
        Log.d(TAG, "onFailedToRecycleView: $viewHolder")
        return super.onFailedToRecycleView(viewHolder)
    }*/

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