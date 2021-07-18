package com.hyh.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


abstract class IItemData<VH : RecyclerView.ViewHolder> {

    internal val delegate = object : Delegate() {

        override fun onDataAttached() {
            super.onDataAttached()
            this@IItemData.onDataAttached()
        }

        override fun onDataActivated() {
            this@IItemData.onDataActivated()
        }

        override fun updateItemData(newItemData: ItemData, payload: Any?) {
            this@IItemData.onUpdateItemData(newItemData)
        }

        override fun onDataInactivated() {
            this@IItemData.onDataInactivated()
        }

        override fun onDataDetached() {
            super.onDataDetached()
            this@IItemData.onDataDetached()
        }
    }

    val displayedItemsSnapshot: List<ItemData>?
        get() = run {
            val displayedItems = delegate.displayedItems
            if (displayedItems == null) null else mutableListOf<ItemData>().apply {
                addAll(displayedItems)
            }
        }

    val localPosition
        get() = delegate.displayedItems?.indexOf(this) ?: -1

    open fun onDataAttached() {}

    open fun onDataActivated() {}

    open fun isSupportUpdateItemData() = false

    open fun onUpdateItemData(newItemData: ItemData) {}

    abstract fun getItemViewType(): Int

    abstract fun getViewHolderFactory(): TypedViewHolderFactory<VH>

    abstract fun onBindViewHolder(viewHolder: VH)

    open fun onBindViewHolder(viewHolder: VH, payloads: List<Any>) = onBindViewHolder(viewHolder)

    /**
     * 判断是否为同一条数据.
     *
     * 例如，使用数据的唯一id作为判断是否为同一条数据的依据.
     */
    abstract fun areItemsTheSame(newItemData: ItemData): Boolean

    /**
     * 判断内容是否改变
     */
    abstract fun areContentsTheSame(newItemData: ItemData): Boolean

    /**
     * 获取数据变动部分
     */
    open fun getChangePayload(newItemData: ItemData): Any? = null

    open fun onDataInactivated() {}

    open fun onDataDetached() {}

    internal abstract class Delegate {

        private var _attached = false
        val attached
            get() = _attached

        var cached = false

        var displayedItems: List<ItemData>? = null

        open fun onDataAttached() {
            _attached = true
        }

        abstract fun onDataActivated()

        abstract fun updateItemData(newItemData: ItemData, payload: Any?)

        abstract fun onDataInactivated()

        open fun onDataDetached() {
            _attached = false
        }
    }
}

typealias ItemData = IItemData<out RecyclerView.ViewHolder>
typealias TypedViewHolderFactory<VH> = (parent: ViewGroup) -> VH
typealias ViewHolderFactory = TypedViewHolderFactory<out RecyclerView.ViewHolder>