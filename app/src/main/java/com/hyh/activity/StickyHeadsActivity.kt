package com.hyh.activity

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.demo.R
import com.hyh.feeds.EventData
import com.hyh.widget.TestItemDecoration
import com.hyh.widget.sticky.IStickyHeadersAdapter
import com.scwang.smart.refresh.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_sticky_heads.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/1
 */
class StickyHeadsActivity : AppCompatActivity() {

    private var mAdapter: ListAdapter = ListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticky_heads)
        recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler_view.adapter = mAdapter
        sticky_headers_layout.setup(
            recycler_view,
            recycler_view.adapter as IStickyHeadersAdapter<*>
        )
        /*recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))*/
        recycler_view.addItemDecoration(TestItemDecoration())
        recycler_view.addItemDecoration(TestItemDecoration())
        recycler_view.addItemDecoration(TestItemDecoration())
        recycler_view.addItemDecoration(TestItemDecoration())

        test(EventData().apply {

        })
        smart_refresh_layout.setRefreshHeader(ClassicsHeader(this))
        smart_refresh_layout.setEnableRefresh(true)
        smart_refresh_layout.setOnRefreshListener {
            smart_refresh_layout.postDelayed(2000) {
                smart_refresh_layout.finishRefresh(true)
            }
        }
    }

    private fun test(eventData: EventData) {

    }

    fun removeItem(v: View) {
        mAdapter.remove()
    }

    fun addItem(v: View) {
        mAdapter.add()
    }

    fun updateItem(v: View) {
        mAdapter.update()
    }

    fun refresh(v: View) {
        smart_refresh_layout.autoRefresh()
    }
}

class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    IStickyHeadersAdapter<RecyclerView.ViewHolder> {

    var mData = mutableListOf<Int>()
    private var mNum = 0

    init {
        for (index in 0..100) {
            mData.add(index)
        }
    }

    fun remove() {
        mData.removeAt(10)
        notifyItemRemoved(10)
    }

    fun add() {
        mData.add(10, 10)
        notifyItemInserted(10)
    }

    fun update() {
        mNum++
        notifyItemChanged(30)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val item = SwitchCompat(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 40, 0, 40)
                textSize = 20F
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
            }
            return object : RecyclerView.ViewHolder(item) {}
        }
        val item = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 40, 0, 40)
            textSize = 20F
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
        }
        return object : RecyclerView.ViewHolder(item) {}
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun getItemViewType(position: Int): Int {
        val data = mData[position]
        if (data % 10 == 0) return 1
        return super.getItemViewType(position)
    }

    val mCheckedMap: HashMap<Int, Boolean> = HashMap()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = mData[position]
        if (data % 10 == 0) {
            holder.itemView.background = ColorDrawable(Color.RED)
        } else {
            holder.itemView.background = ColorDrawable(Color.WHITE)
        }
        Log.d(
            "StickyHeadsActivity_",
            "onBindViewHolder: position = $position, data = $data , ${holder.itemView}"
        )
        if (getItemViewType(position) == 1) {
            (holder.itemView as TextView).setText("条目：$data - $mNum")

            (holder.itemView as SwitchCompat).setOnCheckedChangeListener { buttonView, isChecked ->
                mCheckedMap.put(data, isChecked)
            }
            (holder.itemView as SwitchCompat).isChecked = mCheckedMap.get(data) ?: false
        } else {
            (holder.itemView as TextView).setText("条目：$data")
        }
    }

    override fun isStickyHeader(position: Int): Boolean {
        val data = mData[position]
        return data % 10 == 0
    }

    override fun onBindStickyViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position)

        holder.layoutPosition
        holder.absoluteAdapterPosition

        //viewHolder.itemView.background = ColorDrawable(Color.BLUE)
    }
}