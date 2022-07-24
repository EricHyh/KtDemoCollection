package com.hyh.paging3demo.list.fragment

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.RefreshActuator
import com.hyh.list.*
import com.hyh.list.adapter.MultiItemSourceAdapter
import com.hyh.list.decoration.MultiSourceCardDecoration
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class TradeTabPageFragment : Fragment() {

    companion object {
        private const val TAG = "TradeTabPageFragment"

        var withItemAnimator = false
    }


    private val multiItemSourceAdapter: MultiItemSourceAdapter<Unit> by lazy {
        MultiItemSourceAdapter<Unit>(pageContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade_tab_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.btn_refresh).text = "刷新账户列表\n(随机生成账户卡片列表)"
        view.findViewById<TextView>(R.id.btn_refresh).setOnClickListener {
            multiItemSourceAdapter.refreshRepo(Unit)
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (!withItemAnimator) {
            recyclerView.itemAnimator = null
        }

        //recyclerView.addItemDecoration(SingleSourceFrameDecoration(40, 20F, 0xFFEEEEEE.toInt()))
        recyclerView.adapter = multiItemSourceAdapter
        multiItemSourceAdapter.submitData(TradeTabItemSourceRepo().flow)


        lifecycleScope.launch {
            multiItemSourceAdapter.repoLoadStateFlow.collect {
                Log.d(TAG, "onViewCreated repoLoadStateFlow 1: $it")
            }
        }

        val multiSourceFrameDecoration = MultiSourceCardDecoration(40, 20F, 0xFFEEEEEE.toInt())
        recyclerView.addItemDecoration(multiSourceFrameDecoration)

        lifecycleScope.launch {
            multiItemSourceAdapter.repoLoadStateFlow.collect {
                Log.d(TAG, "onViewCreated repoLoadStateFlow 2: $it")
                if (it is RepoLoadState.Success) {
                    recyclerView.scrollToPosition(0)
                    val sourceTokens = multiItemSourceAdapter.sourceTokens
                    val supportedSourceGroups = mutableListOf<List<Any>>()
                    var sourceGroup: MutableList<Any>? = null
                    sourceTokens.forEachIndexed { index, any ->
                        if (sourceGroup == null) {
                            sourceGroup = mutableListOf()
                        }
                        sourceGroup!!.add(any)
                        if (sourceGroup!!.size == 2) {
                            supportedSourceGroups.add(sourceGroup!!)
                            sourceGroup = null
                        }
                    }
                    multiSourceFrameDecoration.setSupportedSourceGroups(supportedSourceGroups)
                    recyclerView.invalidateItemDecorations()
                }
            }
        }
        lifecycleScope.launch {
            multiItemSourceAdapter.sourceLoadStatesFlow.collect {
                Log.d(TAG, "onViewCreated sourceLoadStatesFlow start: $it")
                it.sourceStateMap.forEach { entry ->
                    Log.d(TAG, "onViewCreated sourceLoadStatesFlow: key = ${entry.key}, value = ${entry.value}")
                }
                Log.d(TAG, "onViewCreated sourceLoadStatesFlow end: $it")
            }
        }
    }
}


class TradeTabItemSourceRepo : SimpleItemSourceRepo<Unit>(Unit) {


    private val accountNamesMap = mapOf(
        Pair(0, listOf("港股账户(1111)")),
        Pair(1, listOf("港股账户(1111)", "美股账户(1111)")),
        Pair(2, listOf("港股账户(1111)", "美股账户(1111)", "A股账户(1111)")),
        Pair(3, listOf("港股账户(1111)", "美股账户(1111)", "A股账户(1111)", "新加坡账户(1111)")),
        Pair(4, listOf("港股账户(1111)", "美股账户(1111)", "A股账户(1111)", "新加坡账户(1111)", "期货账户(1111)")),
        Pair(5, listOf("港股账户(1111)", "美股账户(1111)", "A股账户(1111)", "新加坡账户(1111)", "期货账户(1111)", "港元基金账户(1111)")),
        Pair(6, listOf("港股账户(1111)", "美股账户(1111)", "A股账户(1111)", "新加坡账户(1111)", "期货账户(1111)", "港元基金账户(1111)", "美元基金账户(1111)")),
    )


    override suspend fun getCache(param: Unit): CacheResult {
        return CacheResult.Unused
    }

    override suspend fun load(param: Unit): LoadResult {
        val random = Random(System.currentTimeMillis())
        val accountNames = accountNamesMap[abs(random.nextInt() % 7)] ?: emptyList()
        val newAccountNames = accountNames.map {
            Pair(it, random.nextInt())
        }.sortedBy {
            it.second
        }.map {
            it.first
        }

        val sources = newAccountNames.map {
            AccountCardItemSource(it)
        }
        return LoadResult.Success(sources)
    }
}


class AccountCardItemSource(private val accountName: String) : SimpleItemSource<Unit?>(Unit) {

    private val TAG = "AccountCardItemSource"

    private val accountSettingInfo = AccountSettingInfo()

    override val sourceToken: Any
        get() = accountName

    override suspend fun getPreShow(param: Unit?): PreShowResult<FlatListItem> {
        return PreShowResult.Unused()
    }

    override suspend fun load(param: Unit?): LoadResult<FlatListItem> {
        if (!accountSettingInfo.expandPosition) {
            val accountTitleItemData = AccountTitleFlatListItem(accountName, emptyList(), accountSettingInfo, refreshActuator)
            return LoadResult.Success(listOf(accountTitleItemData))
        } else {
            val random = Random(SystemClock.currentThreadTimeMillis())
            val count = random.nextLong(5, 10).toInt()
            val positions = mutableListOf<String>()
            for (index in 0 until count) {
                positions.add("$index")
            }
            positions.sortBy {
                Math.random()
            }
            val accountTitleItemData = AccountTitleFlatListItem(accountName, positions, accountSettingInfo, refreshActuator)
            val positionItemDataList = positions.map {
                AccountPositionFlatListItem(accountName, it)
            }
            return LoadResult.Success(listOf(accountTitleItemData, *positionItemDataList.toTypedArray()))
        }
    }

    override suspend fun getParam(): Unit? {
        return super.getParam()
    }

    companion object {
        var num = 0
    }

    override fun onAttached() {
        super.onAttached()
        num++
        Log.d(TAG, "onAttached: $this, num = $num")
    }

    override fun onDetached() {
        super.onDetached()
        num--
        Log.d(TAG, "onDetached: $this, num = $num")
    }

}

class AccountSettingInfo {
    var expandPosition: Boolean = true
}


class AccountTitleFlatListItem(
    private val accountName: String,
    private val currentPositionSequence: List<String>,
    private val accountSettingInfo: AccountSettingInfo,
    private val refreshActuator: RefreshActuator,
) : IFlatListItem<AccountTitleFlatListItem.AccountTitleHolder>() {

    override fun getItemViewType(): Int {
        return R.layout.item_account_title
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<AccountTitleHolder> {
        return {
            val itemView = LayoutInflater.from(it.context).inflate(R.layout.item_account_title, it, false)
            AccountTitleHolder(itemView)
        }
    }

    override fun onBindViewHolder(
        viewHolder: AccountTitleHolder
    ) {
        viewHolder.tvAccountName.text = "账户名称: $accountName"
        if (currentPositionSequence.isEmpty()) {
            viewHolder.tvCurrentPositionSequence.text = "持仓序列: 空"
        } else {
            viewHolder.tvCurrentPositionSequence.text = "持仓序列: ${currentPositionSequence.reduce { acc, s -> "$acc$s" }}"
        }
        viewHolder.btnRefreshAccount.setOnClickListener {
            refreshActuator(false)
        }
        viewHolder.btnExpandOrCollapsePosition.text = if (accountSettingInfo.expandPosition) "收起持仓" else "展开持仓"
        viewHolder.btnExpandOrCollapsePosition.setOnClickListener {
            accountSettingInfo.expandPosition = !accountSettingInfo.expandPosition
            refreshActuator(true)
        }
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        return this.accountName == (newItem as? AccountTitleFlatListItem)?.accountName
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is AccountTitleFlatListItem) return false
        return currentPositionSequence.toTypedArray().contentEquals(newItem.currentPositionSequence.toTypedArray())
    }

    class AccountTitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAccountName: TextView = itemView.findViewById(R.id.tv_account_name)
        val tvCurrentPositionSequence: TextView = itemView.findViewById(R.id.tv_current_position_sequence)
        val btnRefreshAccount: Button = itemView.findViewById(R.id.btn_refresh_account)
        val btnExpandOrCollapsePosition: Button = itemView.findViewById(R.id.btn_expand_or_collapse_position)
    }
}

class AccountPositionFlatListItem(
    val accountName: String,
    val positionName: String
) : IFlatListItem<AccountPositionFlatListItem.AccountPositionHolder>() {

    override fun getItemViewType(): Int {
        return R.layout.item_account_position
    }

    override fun getViewHolderFactory(): TypedViewHolderFactory<AccountPositionHolder> {
        return {
            val itemView = LayoutInflater.from(it.context).inflate(R.layout.item_account_position, it, false)
            AccountPositionHolder(itemView)
        }
    }

    override fun onBindViewHolder(
        viewHolder: AccountPositionHolder
    ) {
        viewHolder.tvPositionName.text = "持仓: $positionName"
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is AccountPositionFlatListItem) return false
        return this.accountName == newItem.accountName && this.positionName == newItem.positionName
    }

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        return true
    }

    class AccountPositionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPositionName: TextView = itemView.findViewById(R.id.tv_position_name)
    }
}