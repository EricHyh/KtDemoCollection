package com.hyh.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView]中的一个 Item 对象，负责数据及 UI 渲染
 *
 * @param VH [RecyclerView.ViewHolder]泛型
 */
abstract class IFlatListItem<VH : RecyclerView.ViewHolder> {

    internal val delegate = object : Delegate() {

        override fun onItemAttached() {
            super.onItemAttached()
            this@IFlatListItem.onItemAttached()
        }

        override fun onItemActivated() {
            this@IFlatListItem.onItemActivated()
        }

        override fun updateItem(newItem: FlatListItem, payload: Any?) {
            this@IFlatListItem.onUpdateItem(newItem)
        }

        override fun onItemInactivated() {
            this@IFlatListItem.onItemInactivated()
        }

        override fun onItemDetached() {
            super.onItemDetached()
            this@IFlatListItem.onItemDetached()
        }
    }

    /**
     * 当前展示的列表快照，这里的列表指的是[ItemSource]返回的列表
     */
    val displayedItemsSnapshot: List<FlatListItem>?
        get() = run {
            val displayedItems = delegate.displayedItems
            if (displayedItems == null) null else mutableListOf<FlatListItem>().apply {
                addAll(displayedItems)
            }
        }

    /**
     * 自身在列表中的位置
     */
    val localPosition
        get() = delegate.displayedItems?.indexOf(this) ?: -1

    /**
     * 当 Item 开始被使用时的回调，与[onItemDetached]是一对
     */
    open fun onItemAttached() {}

    /**
     * 当 Item 已经被使用时的回调，与[onItemInactivated]是一对
     */
    open fun onItemActivated() {}

    /**
     * Item 是否支持被更新：
     * - 如果为 true ，那么在 [areItemsTheSame] =  true 时会被认为是同一条数据，会执行[onUpdateItem]
     * - 如果为 false ，那么只要数据发生变动就会用数据替换旧数据
     *
     * 一般情况下不需要重新该函数，只有实现一些特殊的场景才有可能需要用到这个功能；
     */
    open fun isSupportUpdateItem() = false

    /**
     * 在这里根据新的数据，更新自身的数据
     *
     * @param newItem 新的数据
     */
    open fun onUpdateItem(newItem: FlatListItem) {}

    /**
     * [RecyclerView] ItemView 的类型
     */
    abstract fun getItemViewType(): Int

    /**
     * 创建[RecyclerView.ViewHolder]的工厂
     *
     * @return
     */
    abstract fun getViewHolderFactory(): TypedViewHolderFactory<VH>

    fun bindViewHolder(viewHolder: VH, payloads: List<Any>) = onBindViewHolder(viewHolder, payloads)

    protected fun onBindViewHolder(viewHolder: VH, payloads: List<Any>) = onBindViewHolder(viewHolder)

    protected abstract fun onBindViewHolder(viewHolder: VH)

    /**
     * 判断是否为同一条数据.
     *
     * 例如，使用数据的唯一id作为判断是否为同一条数据的依据.
     */
    abstract fun areItemsTheSame(newItem: FlatListItem): Boolean

    /**
     * 判断内容是否改变
     */
    abstract fun areContentsTheSame(newItem: FlatListItem): Boolean

    /**
     * 获取数据变动部分
     */
    open fun getChangePayload(newItem: FlatListItem): Any? = null


    /**
     * 当 Item 没有被使用时的回调，与[onItemActivated]是一对，
     * 处于这种状态下的 Item 下次还可能被继续使用，再次被使用时会回调[onItemActivated]。
     */
    open fun onItemInactivated() {}

    /**
     * 当 Item 彻底被移除的回调，与[onItemAttached]是一对，
     * 处于这种状态下的 Item 不会再使用。
     */
    open fun onItemDetached() {}

    internal abstract class Delegate {

        private var _attached = false
        val attached
            get() = _attached

        var cached = false

        var displayedItems: List<FlatListItem>? = null

        open fun onItemAttached() {
            _attached = true
        }

        abstract fun onItemActivated()

        abstract fun updateItem(newItem: FlatListItem, payload: Any?)

        abstract fun onItemInactivated()

        open fun onItemDetached() {
            _attached = false
        }
    }
}

/**
 * 原始的 Item 加上泛型名字太长了，弄个别名
 */
typealias FlatListItem = IFlatListItem<out RecyclerView.ViewHolder>

/**
 * [RecyclerView.ViewHolder]工厂的别名，lambda 表达式不够直观
 */
typealias TypedViewHolderFactory<VH> = (parent: ViewGroup) -> VH

/**
 * 进一步缩短[TypedViewHolderFactory]
 */
typealias ViewHolderFactory = TypedViewHolderFactory<out RecyclerView.ViewHolder>