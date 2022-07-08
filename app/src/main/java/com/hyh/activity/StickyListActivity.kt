package com.hyh.activity

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.hyh.widget.layoutmanager.StickyHeaders
import com.hyh.widget.layoutmanager.StickyHeadersLinearLayoutManager
import com.hyh.sticky.IStickyItemsAdapter

class StickyListActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recyclerView = RecyclerView(this)
        recyclerView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(recyclerView)

        recyclerView.layoutManager = StickyHeadersLinearLayoutManager<StickyHeadersItemsAdapter>(
            this,
            StickyHeadersLinearLayoutManager.VERTICAL,
            false
        )

        recyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            ).apply {
                setDrawable(object : ColorDrawable(Color.BLACK) {
                    override fun getIntrinsicHeight(): Int {
                        return 10
                    }
                })
            })

        recyclerView.adapter = StickyHeadersItemsAdapter()


    }
}

class ItemHolder(item: View) : RecyclerView.ViewHolder(item) {

}

class StickyHeadersItemsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaders, IStickyItemsAdapter<RecyclerView.ViewHolder> {

    val mSwitchMap = mutableMapOf<Int, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val switch = Switch(parent.context)
            switch.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            //switch.setBackgroundColor(0x55FF0000)
            switch.setBackgroundColor(Color.WHITE)
            switch.setPadding(0, 40, 0, 40)
            switch.textSize = 20F
            switch.setTextColor(Color.BLACK)
            switch.gravity = Gravity.CENTER
            return object : RecyclerView.ViewHolder(switch) {}
        }

        val textView = TextView(parent.context)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.setPadding(0, 40, 0, 40)
        textView.textSize = 20F
        textView.setBackgroundColor(Color.WHITE)
        textView.setTextColor(Color.BLACK)
        textView.gravity = Gravity.CENTER
        return object : RecyclerView.ViewHolder(textView) {

        }
    }

    override fun getItemCount(): Int {
        return 100 + 5
    }


    override fun getItemViewType(position: Int): Int {
        if (position % 20 == 0) return 1
        return super.getItemViewType(position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as TextView).text = if (isFixedStickyHeader(position) || isFixedStickyFooter(position)) {
            "固定的悬停条目：$position"
        } else if (isStickyHeader(position) || isStickyFooter(position)) {
            "普通的悬停条目：$position"
        } else {
            "非悬停条目：$position"
        }

        if (holder.itemView is Switch) {
            (holder.itemView as Switch).setOnCheckedChangeListener { _, isChecked ->
                mSwitchMap[position] = isChecked
                Log.d(
                    "StickyHeadersAdapter",
                    "onBindViewHolder[${this.hashCode()}]: $position - $isChecked"
                )
            }
            (holder.itemView as Switch).isChecked = mSwitchMap[position] ?: false
        }
    }

    override fun isStickyHeader(position: Int): Boolean {
        return position % 20 == 0
    }

    override fun onBindStickyViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(viewHolder, position)
    }

    override fun isStickyFooter(position: Int): Boolean {
        return false
    }

    /**
     * 最大悬停头部布局数量
     */
    override val maxStickyHeaders: Int
        get() = 0

    /**
     * 最大悬停底部布局数量
     */
    override val maxStickyFooters: Int
        get() = 0

    /**
     * 最大固定悬停头部布局数量（不会被顶出去）
     */
    override val maxFixedStickyHeaders: Int
        get() = 0

    /**
     * 最大固定悬停底部布局数量（不会被顶出去）
     */
    override val maxFixedStickyFooters: Int
        get() = 0
}