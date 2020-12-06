package com.hyh.activity

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.demo.R
import com.hyh.feeds.EventData
import com.hyh.widget.sticky.IStickyHeadersAdapter
import kotlinx.android.synthetic.main.activity_sticky_heads.*

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/12/1
 */
class StickyHeadsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticky_heads)
        recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler_view.adapter = ListAdapter()
        sticky_headers_layout.setup(recycler_view, recycler_view.adapter as IStickyHeadersAdapter<*>)
        test(EventData().apply {

        })
    }

    private fun test(eventData: EventData) {

    }
}

class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IStickyHeadersAdapter<RecyclerView.ViewHolder> {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val item = SwitchCompat(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(0, 40, 0, 40)
                textSize = 20F
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
            }
            return object : RecyclerView.ViewHolder(item) {}
        }
        val item = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, 40, 0, 40)
            textSize = 20F
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
        }
        return object : RecyclerView.ViewHolder(item) {}
    }

    override fun getItemCount(): Int {
        return 100
    }

    override fun getItemViewType(position: Int): Int {
        if (position % 10 == 0) return 1
        return super.getItemViewType(position)
    }

    val mCheckedMap: HashMap<Int, Boolean> = HashMap()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as TextView).setText("条目：$position")
        if (position % 10 == 0) {
            holder.itemView.background = ColorDrawable(Color.RED)
        } else {
            holder.itemView.background = ColorDrawable(Color.WHITE)
        }
        Log.d("StickyHeadsActivity_", "onBindViewHolder: $position")
        if (getItemViewType(position) == 1) {
            (holder.itemView as SwitchCompat).setOnCheckedChangeListener { buttonView, isChecked ->
                mCheckedMap.put(position, isChecked)
            }
            (holder.itemView as SwitchCompat).isChecked = mCheckedMap.get(position) ?: false
        }
    }

    override fun isStickyHeader(position: Int): Boolean {
        return position % 10 == 0
    }

    override fun onBindStickyViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(viewHolder, position)
        //viewHolder.itemView.background = ColorDrawable(Color.BLUE)
    }

}