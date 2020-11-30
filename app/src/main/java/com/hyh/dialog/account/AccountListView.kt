package com.hyh.dialog.account

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.demo.R
import kotlin.math.roundToInt


class AccountListView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    RelativeLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "AccountListView"
    }


    private var mCloseClickListener: (() -> Unit)? = null

    private var mSelectedListener: ((item: AccountData) -> Unit)? = null

    private var mDataList: MutableList<AccountItemData> = mutableListOf()

    private val mRecyclerView: RecyclerView

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_account_list_view, this)
        setBackgroundColor(0x55FF0000)
        /*findViewById<View>(R.id.iv_close).setOnClickListener {
            mCloseClickListener?.invoke()
        }*/
        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        mRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRecyclerView.adapter = AccountListAdapter()

        //mRecyclerView.addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged -> $measuredHeight")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightPixels = resources.displayMetrics.heightPixels
        if (measuredHeight == (heightPixels * 0.56).roundToInt()
            || measuredHeight == (heightPixels * 0.76).roundToInt()
        ) {
            return
        }
        val newHeightMeasureSpec: Int = if (measuredHeight < (heightPixels * 0.66).roundToInt()) {
            MeasureSpec.makeMeasureSpec((heightPixels * 0.56).roundToInt(), MeasureSpec.EXACTLY)
        } else {
            MeasureSpec.makeMeasureSpec((heightPixels * 0.76).roundToInt(), MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
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
                3 -> {
                    val height = parent.context.resources.displayMetrics.density * 40
                    val view = View(parent.context)
                    view.layoutParams =
                        ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, height.roundToInt())
                    object : RecyclerView.ViewHolder(view) {}
                }
                else -> object : RecyclerView.ViewHolder(View(parent.context)) {}
            }
        }

        override fun getItemCount(): Int {
            return mDataList.size + 1
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position !in 0 until mDataList.size) return
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
            if (position == mDataList.size) return 3
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