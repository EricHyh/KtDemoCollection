package com.hyh.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MultiAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mChildAdapters: MutableList<RecyclerView.Adapter<RecyclerView.ViewHolder>> =
        mutableListOf()

    fun addChildAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
        mChildAdapters.add(adapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int {
        var itemCount = 0
        for (adapter in mChildAdapters) {
            itemCount += adapter.itemCount
        }
        return itemCount
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == RecyclerView.NO_POSITION) {
            return
        }
        var curPosition = position
        for (adapter in mChildAdapters) {
            val itemCount = adapter.itemCount
            if (curPosition <= itemCount - 1) {
                adapter.onBindViewHolder(
                    holder,
                    position
                )
            } else {
                curPosition -= itemCount
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    private fun findChildAdapter(position: Int): RecyclerView.Adapter<out RecyclerView.ViewHolder>? {
        var curPosition = position
        mChildAdapters.forEach {
            val itemCount = it.itemCount
            if (curPosition <= itemCount - 1) {
                return it
            } else {
                curPosition -= itemCount
            }
        }
        return null
    }
}


interface IMultiAdapter {

    fun getChildAdapterPosition(childAdapter: ChildAdapter<*>): Int

    fun notifyDataSetChanged(childAdapter: ChildAdapter<*>, oldSize: Int, currentSize: Int)

    fun notifyItemRemoved(childAdapter: ChildAdapter<*>, index: Int)

    fun notifyItemRangeRemoved(childAdapter: ChildAdapter<*>, positionStart: Int, itemCount: Int)

    fun notifyItemRangeInserted(childAdapter: ChildAdapter<*>, insertPosition: Int, size: Int)

    fun notifyItemChanged(childAdapter: ChildAdapter<*>, index: Int)

    fun notifyItemRangeChanged(childAdapter: ChildAdapter<*>, startIndex: Int, itemCount: Int)

    fun notifyItemRangeChanged(
        childAdapter: ChildAdapter<*>,
        startIndex: Int,
        itemCount: Int,
        payload: Any?
    )

    fun notifyItemMoved(childAdapter: ChildAdapter<*>, fromPosition: Int, toPosition: Int)

}