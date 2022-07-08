package com.hyh.activity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
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
import com.hyh.sticky.IStickyItemsAdapter
import com.hyh.sticky.StickyItemDecoration
import com.hyh.sticky.StickyItemsLayout
import com.hyh.sticky.StickyItemsListener
import com.scwang.smart.refresh.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_sticky_heads.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/1
 */
class StickyHeadsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "_StickyHeadsActivity_"
    }

    private var mAdapter: ListAdapter = ListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticky_heads)
        recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler_view.adapter = mAdapter
        sticky_headers_layout.setup(
            recycler_view,
            recycler_view.adapter as IStickyItemsAdapter<*>
        )

        sticky_headers_layout.setStickyItemDecoration(StickyItemDecorationImpl())

        /*recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))*/
        // recycler_view.addItemDecoration(TestItemDecoration())


        sticky_headers_layout.stickyItemsListener = object : StickyItemsListener {

            override fun onStickItemsAdded(parent: StickyItemsLayout, adapter: IStickyItemsAdapter<*>, position: Int) {
                Log.d(TAG, "onStickItemsAdded: $position")
            }

            override fun onStickItemsRemoved(parent: StickyItemsLayout, adapter: IStickyItemsAdapter<*>, position: Int) {
                Log.d(TAG, "onStickItemsRemoved: $position")
            }

        }

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
    IStickyItemsAdapter<RecyclerView.ViewHolder> {


    /**
     * 最大悬停头部布局数量
     */
    override val maxStickyHeaders: Int
        get() = 4

    /**
     * 最大悬停底部布局数量
     */
    override val maxStickyFooters: Int
        get() = 4

    /**
     * 最大固定悬停头部布局数量（不会被顶出去）
     */
    override val maxFixedStickyHeaders: Int
        get() = 2

    /**
     * 最大固定悬停底部布局数量（不会被顶出去）
     */
    override val maxFixedStickyFooters: Int
        get() = 2


    var mData = mutableListOf<Int>()
    private var mNum = 0


    init {
        for (index in 0..200) {
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
        } else if (viewType == 2) {
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
        //if (position == 13) return 2
        return super.getItemViewType(position)
    }

    val mCheckedMap: HashMap<Int, Boolean> = HashMap()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = mData[position]
        if (isStickyHeader(position)) {
            holder.itemView.background = ColorDrawable(Color.RED)
        } else if (isStickyFooter(position)) {
            holder.itemView.background = ColorDrawable(Color.BLUE)
        } else {
            holder.itemView.background = ColorDrawable(Color.WHITE)
        }

        Log.d(
            "StickyHeadsActivity_",
            "onBindViewHolder: position = $position, data = $data , ${holder.itemView}"
        )
        if (getItemViewType(position) == 1) {

            (holder.itemView as TextView).text = if (isFixedStickyHeader(position) || isFixedStickyFooter(position)) {
                "固定的悬停条目：$data - $mNum"
            } else if (isStickyHeader(position) || isStickyFooter(position)) {
                "普通的悬停条目：$data - $mNum"
            } else {
                "非悬停条目：$data - $mNum"
            }


            //(holder.itemView as TextView).text = "条目：$data - $mNum"

            (holder.itemView as SwitchCompat).setOnCheckedChangeListener { buttonView, isChecked ->
                mCheckedMap.put(data, isChecked)
            }
            (holder.itemView as SwitchCompat).isChecked = mCheckedMap.get(data) ?: false
        } else {
            (holder.itemView as TextView).text = if (isFixedStickyHeader(position) || isFixedStickyFooter(position)) {
                "固定的悬停条目：$data"
            } else if (isStickyHeader(position) || isStickyFooter(position)) {
                "普通的悬停条目：$data"
            } else {
                "非悬停条目：$data"
            }
            //(holder.itemView as TextView).setText("条目：$data")
        }
    }

    override fun isStickyHeader(position: Int): Boolean {
        val data = mData[position]
        return data % 10 == 0 /*|| position == 13*/
        //return position == 0
    }

    override fun onBindStickyViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position)
        //holder.itemView.background = ColorDrawable(Color.BLUE)
    }

    override fun isStickyFooter(position: Int): Boolean {
        return !isStickyHeader(position) && let {
            val data = mData[position]
            data % 17 == 0
        }
        //return position == itemCount -1
    }

    override fun isFixedStickyHeader(position: Int): Boolean {
        val data = mData[position]
        return data == 20 || data == 40
        //return false
    }

    override fun isFixedStickyFooter(position: Int): Boolean {
        val data = mData[position]
        return data == 68 || data == 102
    }
}


class StickyItemDecorationImpl : StickyItemDecoration {

    override fun onDrawOver(c: Canvas, parent: StickyItemsLayout) {

    }

    override fun onDraw(c: Canvas, parent: StickyItemsLayout) {

    }

    override fun getItemOffsets(outRect: Rect, adapterPosition: Int, parent: StickyItemsLayout) {
        //outRect.set(20, 20, 20, 20)
    }

}