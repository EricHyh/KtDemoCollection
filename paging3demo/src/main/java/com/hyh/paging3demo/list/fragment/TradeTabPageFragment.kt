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
import com.hyh.list.adapter.SourceRepoAdapter
import com.hyh.list.decoration.ItemSourceFrameDecoration
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


    private val sourceRepoAdapter: SourceRepoAdapter<Unit> by lazy {
        SourceRepoAdapter<Unit>(pageContext)
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
            sourceRepoAdapter.refreshRepo(Unit)
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (!withItemAnimator) {
            recyclerView.itemAnimator = null
        }

        recyclerView.addItemDecoration(ItemSourceFrameDecoration(40, 20F, 0xFFEEEEEE.toInt()))
        recyclerView.adapter = sourceRepoAdapter
        sourceRepoAdapter.submitData(TradeTabItemSourceRepo().flow)


        lifecycleScope.launch {
            sourceRepoAdapter.repoLoadStateFlow.collect {
                Log.d(TAG, "onViewCreated repoLoadStateFlow 1: $it")
            }
        }

        lifecycleScope.launch {
            sourceRepoAdapter.repoLoadStateFlow.collect {
                Log.d(TAG, "onViewCreated repoLoadStateFlow 2: $it")
                if (it is RepoLoadState.Success) {
                    recyclerView.scrollToPosition(0)
                    lifecycleScope.launch {
                        sourceRepoAdapter.getSourceLoadState(0)?.collect {
                            Log.d(TAG, "onViewCreated SourceLoadState 1: $it")
                        }
                    }

                    lifecycleScope.launch {
                        sourceRepoAdapter.getSourceLoadState(0)?.collect {
                            Log.d(TAG, "onViewCreated SourceLoadState 2: $it")
                        }
                    }
                }
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


class AccountCardItemSource(private val accountName: String) : SimpleItemSource<Unit>() {

    private val TAG = "AccountCardItemSource"

    private val accountSettingInfo = AccountSettingInfo()

    override val sourceToken: Any
        get() = accountName

    override suspend fun getPreShow(param: Unit): PreShowResult<ItemData> {
        return PreShowResult.Unused()
    }

    override suspend fun load(param: Unit): LoadResult<ItemData> {
        if (!accountSettingInfo.expandPosition) {
            val accountTitleItemData = AccountTitleItemData(accountName, emptyList(), accountSettingInfo, refreshActuator)
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
            val accountTitleItemData = AccountTitleItemData(accountName, positions, accountSettingInfo, refreshActuator)
            val positionItemDataList = positions.map {
                AccountPositionItemData(accountName, it)
            }
            return LoadResult.Success(listOf(accountTitleItemData, *positionItemDataList.toTypedArray()))
        }
    }

    override suspend fun getParam() {}

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


class AccountTitleItemData(
    private val accountName: String,
    private val currentPositionSequence: List<String>,
    private val accountSettingInfo: AccountSettingInfo,
    private val refreshActuator: RefreshActuator,
) : IItemData<AccountTitleItemData.AccountTitleHolder>() {

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

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        return this.accountName == (newItemData as? AccountTitleItemData)?.accountName
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is AccountTitleItemData) return false
        return currentPositionSequence.toTypedArray().contentEquals(newItemData.currentPositionSequence.toTypedArray())
    }

    class AccountTitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAccountName: TextView = itemView.findViewById(R.id.tv_account_name)
        val tvCurrentPositionSequence: TextView = itemView.findViewById(R.id.tv_current_position_sequence)
        val btnRefreshAccount: Button = itemView.findViewById(R.id.btn_refresh_account)
        val btnExpandOrCollapsePosition: Button = itemView.findViewById(R.id.btn_expand_or_collapse_position)
    }
}

class AccountPositionItemData(
    val accountName: String,
    val positionName: String
) : IItemData<AccountPositionItemData.AccountPositionHolder>() {

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

    override fun areItemsTheSame(newItemData: ItemData): Boolean {
        if (newItemData !is AccountPositionItemData) return false
        return this.accountName == newItemData.accountName && this.positionName == newItemData.positionName
    }

    override fun areContentsTheSame(newItemData: ItemData): Boolean {
        return true
    }

    class AccountPositionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPositionName: TextView = itemView.findViewById(R.id.tv_position_name)
    }
}