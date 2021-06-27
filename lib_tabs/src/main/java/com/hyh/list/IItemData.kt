package com.hyh.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


abstract class IItemData<VH : RecyclerView.ViewHolder> {

    internal val delegate = object : Delegate() {

        override fun onAttached() {
            super.onAttached()
        }

        override fun onActivated() {
        }

        override fun updateItemData(newItemData: ItemData) {
            onUpdateItemData(newItemData)
        }

        override fun onInactivated() {
        }

        override fun onDetached() {
            super.onDetached()
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

    /**
     * 数据被激活时回调
     */
    open fun onActivated() {}

    open fun isSupportUpdateItemData() = false

    open fun onUpdateItemData(newItemData: ItemData) {}

    abstract fun getItemViewType(): Int

    abstract fun getViewHolderFactory(): TypedViewHolderFactory<VH>

    abstract fun onBindViewHolder(viewHolder: VH)

    open fun onBindViewHolder(viewHolder: VH, payloads: MutableList<Any>) = onBindViewHolder(viewHolder)

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

    /**
     * 数据不再使用时回调
     */
    open fun onDetached() {}

    internal abstract class Delegate {

        private var _attached = false
        val attached
            get() = _attached

        var cached = false

        var displayedItems: List<ItemData>? = null

        open fun onAttached() {
            _attached = true
        }

        abstract fun onActivated()
        abstract fun updateItemData(newItemData: ItemData)
        abstract fun onInactivated()

        open fun onDetached() {
            _attached = false
        }
    }
}

typealias ItemData = IItemData<out RecyclerView.ViewHolder>
typealias TypedViewHolderFactory<VH> = (parent: ViewGroup) -> VH
typealias ViewHolderFactory = TypedViewHolderFactory<out RecyclerView.ViewHolder>