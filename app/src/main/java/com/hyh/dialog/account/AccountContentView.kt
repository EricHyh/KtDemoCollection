package com.hyh.dialog.account

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.demo.R
import com.hyh.dialog.core.IContentView
import com.hyh.dialog.core.WindowInterface

class AccountContentView : IContentView<OnAccountSelectedListener> {

    private lateinit var windowInterface: WindowInterface
    private var listener: OnAccountSelectedListener? = null

    override fun setup(windowInterface: WindowInterface, t: OnAccountSelectedListener?) {
        this.windowInterface = windowInterface
        this.listener = t
    }


    override fun onCreateView(context: Context, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.account_dialog, parent, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        return view
    }

    override fun onDestroyView() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class AccountAdapter(
    val accountGroups: List<AccountGroup>,
    val listener: OnAccountSelectedListener?
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val mItemCount: Int by lazy {
        var count = accountGroups.size
        accountGroups.forEach {
            count += it.accounts.size
        }
        count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(View(parent.context)) {}

    }

    override fun getItemCount(): Int {
        return mItemCount
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }
}

interface OnAccountSelectedListener {

    fun onAccountSelected(account: String)

}