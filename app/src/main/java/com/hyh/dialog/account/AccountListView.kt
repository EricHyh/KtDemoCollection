package com.hyh.dialog.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.dialog.R
import kotlin.math.roundToInt


class AccountListView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    private var mCloseClickListener: (() -> Unit)? = null

    private var mSelectedListener: ((item: AccountData) -> Unit)? = null

    private var mDataList: MutableList<AccountItemData> = mutableListOf()

    private val mRecyclerView: RecyclerView

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_account_list_view, this)
        findViewById<View>(R.id.iv_close).setOnClickListener {
            mCloseClickListener?.invoke()
        }
        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        mRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRecyclerView.adapter = AccountListAdapter()

        //mRecyclerView.addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
    }

    fun setCloseClickListener(listener: () -> Unit) {
        this.mCloseClickListener = listener
    }

    fun setSelectedListener(listener: (item: AccountData) -> Unit) {
        this.mSelectedListener = listener
    }

    fun setAccountGroups(groups: List<AccountGroup>, accountId: Long?) {
        mDataList.clear()
        groups.forEach { group ->
            mDataList.add(AccountItemData(1, BrokerTitle(group.brokerId)))
            group.accounts.forEach { data ->
                val selected = (accountId == data.accountId)
                mDataList.add(
                    AccountItemData(
                        2,
                        AccountItem(group.brokerId, data.account, data.accountId, selected)
                    )
                )
            }
        }
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    private inner class AccountListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                1 -> BrokerTitleHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_account_list_broker_title,
                        parent,
                        false
                    )
                )
                2 -> AccountItemHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_account_list_account,
                        parent,
                        false
                    )
                ) {
                    mSelectedListener?.invoke(
                        AccountData(
                            it.brokerId,
                            it.account,
                            it.accountId
                        )
                    )
                    mCloseClickListener?.invoke()
                }
                else -> object : RecyclerView.ViewHolder(View(parent.context)) {}
            }
        }

        override fun getItemCount(): Int {
            return mDataList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            mDataList[position].let {
                if (it.type == 1) {
                    val paddingTop =
                        if (position == 0) {
                            (holder.itemView.resources.displayMetrics.density * 20).roundToInt()
                        } else {
                            (holder.itemView.resources.displayMetrics.density * 22).roundToInt()
                        }
                    holder.itemView.setPadding(
                        holder.itemView.paddingLeft,
                        paddingTop,
                        holder.itemView.paddingRight,
                        holder.itemView.paddingBottom
                    )
                    (holder as BrokerTitleHolder).bindViewHolder(it.data as BrokerTitle)
                } else if (it.type == 2) {
                    (holder as AccountItemHolder).bindViewHolder(it.data as AccountItem)
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return mDataList[position].type
        }
    }
}

private class BrokerTitleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val tvTitle: TextView = itemView.findViewById(R.id.tv_title)

    fun bindViewHolder(title: BrokerTitle) {
        tvTitle.text = (title.brokerId.toString())
    }
}

private class AccountItemHolder(
    itemView: View,
    val itemClickListener: (accountItem: AccountItem) -> Unit
) :
    RecyclerView.ViewHolder(itemView) {

    private val accountIcon: ImageView = itemView.findViewById(R.id.iv_account_icon)
    private val accountName: TextView = itemView.findViewById(R.id.tv_account_name)
    private val accountSelected: ImageView = itemView.findViewById(R.id.iv_account_selected)

    fun bindViewHolder(accountItem: AccountItem) {
        itemView.setOnClickListener {
            itemClickListener(accountItem)
        }
        accountName.text = accountItem.accountId.toString()
        accountSelected.visibility = if (accountItem.selected) View.VISIBLE else View.GONE
    }
}

private data class AccountItemData(
    val type: Int,
    val data: Any
)


private data class BrokerTitle(
    val brokerId: Int
)

private data class AccountItem(
    val brokerId: Int,
    val account: AccountType,
    val accountId: Long,
    var selected: Boolean = false
)