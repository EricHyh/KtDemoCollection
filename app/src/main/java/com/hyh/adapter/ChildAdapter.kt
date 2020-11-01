package com.hyh.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.min

abstract class ChildAdapter<T> : RecyclerView.Adapter<ItemHolder<T>>() {

    protected lateinit var mContext: Context
    protected lateinit var mMultiAdapter: IMultiAdapter

    private var mList: MutableList<T>? = null


    fun getList(): MutableList<T>? {
        return mList
    }

    protected fun setup(context: Context, multiAdapter: IMultiAdapter) {
        this.mContext = context
        this.mMultiAdapter = multiAdapter
        registerAdapterDataObserver(ChildAdapterDataObserver())
    }

    override fun onBindViewHolder(holder: ItemHolder<T>, position: Int) {
        mList?.let { list ->
            if (list.size > position) {
                holder.onBindViewHolder(list[position], {
                    list.indexOf(it)
                })
            }
        }
    }

    override fun onBindViewHolder(
        holder: ItemHolder<T>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            mList?.let {

            }
        }
    }

    override fun getItemCount(): Int {
        return mList?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    fun setDataList(list: List<T>?) {
        val oldSize = mList?.size ?: 0
        this.mList = if (list == null) null else ArrayList(list)
        val currentSize = list?.size ?: 0

        /*DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            }

            override fun getOldListSize(): Int {

            }

            override fun getNewListSize(): Int {
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                return super.getChangePayload(oldItemPosition, newItemPosition)
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            }
        })*/

        mMultiAdapter.notifyDataSetChanged(
            this,
            oldSize,
            currentSize
        )
    }

    fun addDataList(list: List<T>?) {
        if (list.isNullOrEmpty()) {
            return
        }
        if (this.mList == null) {
            setDataList(list)
        } else {
            val oldSize = mList?.size ?: 0
            mList?.addAll(list)
            mMultiAdapter.notifyItemRangeInserted(
                this,
                oldSize,
                list.size
            )
        }
    }

    fun addData(t: T?) {
        if (t == null) {
            return
        }
        if (this.mList == null) {
            val list = ArrayList<T>()
            list.add(t)
            setDataList(list)
        } else {
            val oldSize = mList?.size ?: 0
            mList?.add(t)
            mMultiAdapter.notifyItemRangeInserted(
                this,
                oldSize,
                1
            )
        }
    }

    fun insertDataList(list: List<T>?, position: Int) {
        if (list == null || list.isEmpty()) {
            return
        }
        if (this.mList == null) {
            setDataList(list)
        } else {
            val oldSize = mList?.size ?: 0
            val insertPosition = if (position >= oldSize) oldSize else position
            mList?.addAll(insertPosition, list)
            mMultiAdapter.notifyItemRangeInserted(
                this,
                insertPosition,
                list.size
            )
        }
    }

    fun insertData(t: T?, position: Int): Boolean {
        if (t == null) {
            return false
        }
        if (this.mList == null) {
            val list = ArrayList<T>()
            list.add(t)
            setDataList(list)
            return true
        } else {
            val oldSize = mList?.size ?: 0
            if (position <= oldSize) {
                mList?.add(position, t)
                mMultiAdapter.notifyItemRangeInserted(
                    this,
                    position,
                    1
                )
                return true
            }
        }
        return false
    }

    fun removeDataByPosition(position: Int): T? {
        if (this.mList == null || position >= mList?.size ?: 0) {
            return null
        }
        val remove = this.mList?.removeAt(position)
        mMultiAdapter.notifyItemRemoved(this, position)
        return remove
    }

    fun removeData(t: T): Int {
        val index = this.mList?.indexOf(t) ?: -1
        if (index < 0) {
            return index
        }
        mList?.remove(t)
        mMultiAdapter.notifyItemRemoved(this, index)
        return index
    }

    fun updateItem(index: Int) {
        if (this.mList == null || index >= mList?.size ?: 0) {
            return
        }
        mMultiAdapter.notifyItemChanged(this, index)
    }

    fun updateItem(t: T, index: Int) {
        if (mList == null || index >= mList?.size ?: 0) {
            return
        }
        mList?.set(index, t)
        //this.mList?.removeAt(index)
        //this.mList?.add(index, t)
        mMultiAdapter.notifyItemChanged(this, index)
    }

    fun updateRangeItem(startIndex: Int, itemCount: Int) {
        if (this.mList == null || startIndex >= mList?.size ?: 0) {
            return
        }

        val newItemCount = min(itemCount, this.mList?.size ?: 0 - startIndex)

        mMultiAdapter.notifyItemRangeChanged(
            this,
            startIndex,
            newItemCount
        )
    }

    fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        return mList?.let { list ->
            if (list.size <= fromPosition || list.size <= toPosition) {
                false
            } else {
                if (toPosition != fromPosition) {
                    list.add(toPosition, list.removeAt(fromPosition))
                }
                true
            }
        } ?: false
    }

    protected fun onDestroy() {
        val list = mList
        mList = null
        list?.clear()
    }


    inner class ChildAdapterDataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            mMultiAdapter.notifyItemRangeChanged(
                this@ChildAdapter,
                0,
                itemCount
            )
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            mMultiAdapter.notifyItemRangeRemoved(
                this@ChildAdapter,
                positionStart,
                itemCount
            )
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            mMultiAdapter.notifyItemMoved(
                this@ChildAdapter,
                fromPosition,
                toPosition
            )
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            mMultiAdapter.notifyItemRangeInserted(
                this@ChildAdapter,
                positionStart,
                itemCount
            )
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            mMultiAdapter.notifyItemRangeChanged(
                this@ChildAdapter,
                positionStart,
                itemCount
            )
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            mMultiAdapter.notifyItemRangeChanged(
                this@ChildAdapter,
                positionStart,
                itemCount,
                payload
            )
        }
    }
}