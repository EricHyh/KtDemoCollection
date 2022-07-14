package com.hyh.list

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.hyh.lifecycle.ChildLifecycleOwner
import com.hyh.lifecycle.IChildLifecycleOwner
import com.hyh.tabs.BuildConfig
import com.hyh.tabs.R
import java.lang.ref.WeakReference

/**
 * [RecyclerView]中的一个 Item 对象，负责数据及 UI 渲染
 *
 * @param VH [RecyclerView.ViewHolder]泛型
 */
abstract class IFlatListItem<VH : RecyclerView.ViewHolder> : LifecycleOwner {

    companion object {
        private const val TAG = "IFlatListItem"
    }

    internal val delegate = object : Delegate<VH>() {

        override fun bindParentLifecycle(lifecycle: Lifecycle) {
            if (enableParentLifecycle()) {
                super.bindParentLifecycle(lifecycle)
            }
        }

        override fun onItemAttached() {
            super.onItemAttached()
            this@IFlatListItem.onItemAttached()
        }

        override fun onItemActivated() {
            super.onItemActivated()
            this@IFlatListItem.onItemActivated()
        }

        override fun updateItem(newItem: FlatListItem, payload: Any?) {
            this@IFlatListItem.onUpdateItem(newItem)
        }

        override fun onItemInactivated() {
            super.onItemInactivated()
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

    val attachedSourceToken: Any?
        get() = delegate.attachedSourceToken

    /**
     * 自身在列表中的位置
     */
    val localPosition
        get() = delegate.displayedItems?.indexOf(this) ?: -1


    /**
     * 获取[IFlatListItem]对象的什么周期，各生命周期状态的描述如下：
     * - [Lifecycle.State.INITIALIZED]：Item 被创建后的初始状态
     * - [Lifecycle.State.CREATED]：Item 在缓存中，还未被添加到列表中的状态，对应回调[onItemAttached]
     * - [Lifecycle.State.STARTED]：Item 被添加到列表中的状态，对应回调[onItemActivated]
     * - [Lifecycle.State.RESUMED]：Item 绑定的 ItemView 添加到 ViewTree 上的状态，对应回调[onViewAttachedToWindow]
     * - [Lifecycle.State.DESTROYED]：Item 被销毁的状态，对应回调[onItemDetached]
     *
     * 特别的，Item 的 Lifecycle 最终绑定了 外层页面（如：Fragment）的 Lifecycle。例如：
     * item 自身的生命周期状态为[Lifecycle.State.RESUMED]，Fragment 的生命周期状态从[Lifecycle.State.RESUMED]，
     * 切换到了[Lifecycle.State.STARTED]，那么 Item 的 Lifecycle 也会切换到[Lifecycle.State.STARTED]
     */
    final override fun getLifecycle(): Lifecycle {
        return delegate.lifecycleOwner.lifecycle
    }

    protected open fun enableParentLifecycle(): Boolean {
        return false
    }

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
     * - 如果为 false ，那么只要数据发生变动就会用新数据替换旧数据
     *
     * 一般情况下不需要重写该函数，只有实现一些特殊的场景才有可能需要用到这个功能；
     */
    open fun isSupportUpdateItem() = false

    /**
     * 当[isSupportUpdateItem] = true，并且 [areItemsTheSame] = true 时，会回调该函数，
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

    fun bindViewHolder(viewHolder: VH, payloads: List<Any>) {
        delegate.onBindItemView(viewHolder.itemView)
        onBindViewHolder(viewHolder, payloads)
    }

    protected open fun onBindViewHolder(viewHolder: VH, payloads: List<Any>) =
        onBindViewHolder(viewHolder)

    protected abstract fun onBindViewHolder(viewHolder: VH)

    internal fun viewAttachedToWindow(viewHolder: VH) {
        onViewAttachedToWindow(viewHolder)
    }

    protected open fun onViewAttachedToWindow(viewHolder: VH) {}

    internal fun viewDetachedFromWindow(viewHolder: VH) {
        onViewDetachedFromWindow(viewHolder)
    }

    protected open fun onViewDetachedFromWindow(viewHolder: VH) {}

    internal fun viewRecycled(viewHolder: VH) = onViewRecycled(viewHolder)
    protected open fun onViewRecycled(viewHolder: VH) {}

    internal fun failedToRecycleView(viewHolder: VH): Boolean = onFailedToRecycleView(viewHolder)
    protected open fun onFailedToRecycleView(viewHolder: VH): Boolean = false

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

    internal abstract inner class Delegate<VH : RecyclerView.ViewHolder> :
        DataDelegate<VH>,
        IChildLifecycleOwner,
        View.OnAttachStateChangeListener {

        override val lifecycleOwner: ChildLifecycleOwner = ChildLifecycleOwner()

        @Volatile
        var attachedSourceToken: Any? = null

        private var _attached = false
        override val attached
            get() = _attached

        var cached = false

        var displayedItems: List<FlatListItem>? = null

        private val boundItemViewRefSet = mutableSetOf<VHReference>()

        @CallSuper
        override fun onItemAttached() {
            _attached = true
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
            Log.d(
                TAG,
                "onItemAttached: ${this@IFlatListItem}, " +
                        "currentSelfState=${lifecycleOwner.lifecycle.selfState}, " +
                        "currentState=${lifecycleOwner.lifecycle.currentState}"
            )
        }

        @CallSuper
        override fun onItemActivated() {
            val currentSelfState = lifecycleOwner.lifecycle.selfState
            Log.d(
                TAG,
                "onItemActivated: ${this@IFlatListItem}, " +
                        "currentSelfState=$currentSelfState, " +
                        "currentState=${lifecycleOwner.lifecycle.currentState}"
            )

            if (currentSelfState != Lifecycle.State.CREATED) {
                val errorMsg =
                    getErrorMsg("onItemActivated", currentSelfState, Lifecycle.State.CREATED)
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException(errorMsg)
                } else {
                    Log.e(TAG, errorMsg)
                }
            }

            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
        }

        @Suppress("UNCHECKED_CAST")
        fun onBindItemView(itemView: View) {
            val boundItem = itemView.getBoundItem() as? IFlatListItem<VH>
            if (boundItem === this@IFlatListItem) return
            boundItem?.delegate?.onUnBindItemView(itemView)

            val isAttached = itemView.isAttachedToWindow
            Log.d(
                TAG,
                "onBindItemView: ${this@IFlatListItem}, " +
                        "currentSelfState=${lifecycleOwner.lifecycle.selfState}, " +
                        "currentState=${lifecycleOwner.lifecycle.currentState}, " +
                        "isAttached=${isAttached}"
            )
            if (isAttached) {
                lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
            }

            itemView.addOnAttachStateChangeListener(this)
            boundItemViewRefSet.add(VHReference(itemView))

            itemView.setBoundItem(this@IFlatListItem)
        }

        private fun onUnBindItemView(itemView: View) {

            itemView.removeOnAttachStateChangeListener(this)
            val vhReference = VHReference(itemView)
            boundItemViewRefSet.remove(vhReference)

            val isAttached = isAttachedToWindow()

            Log.d(
                TAG,
                "onUnBindItemView: ${this@IFlatListItem}, " +
                        "currentSelfState=${lifecycleOwner.lifecycle.selfState}, " +
                        "currentState=${lifecycleOwner.lifecycle.currentState}, " +
                        "isAttached=${isAttached}"
            )

            if (!isAttached) {
                val currentSelfState = lifecycleOwner.lifecycle.selfState
                if (currentSelfState == Lifecycle.State.RESUMED) {
                    lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
                }
            }
        }


        override fun onViewAttachedToWindow(itemView: View) {
            val boundItem = itemView.getBoundItem()
            if (boundItem === this@IFlatListItem) {
                val currentSelfState = lifecycleOwner.lifecycle.selfState
                if (currentSelfState < Lifecycle.State.STARTED) {
                    val errorMsg =
                        getErrorMsg("onViewAttachedToWindow", currentSelfState, Lifecycle.State.STARTED)
                    if (BuildConfig.DEBUG) {
                        throw IllegalStateException(errorMsg)
                    } else {
                        Log.e(TAG, errorMsg)
                    }
                }

                Log.d(
                    TAG,
                    "onViewAttachedToWindow: ${this@IFlatListItem}, " +
                            "currentSelfState=${lifecycleOwner.lifecycle.selfState}, " +
                            "currentState=${lifecycleOwner.lifecycle.currentState}"
                )

                lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
            } else {
                onUnBindItemView(itemView)
            }
        }

        override fun onViewDetachedFromWindow(itemView: View) {
            val boundItem = itemView.getBoundItem()
            if (boundItem === this@IFlatListItem) {
                val isAttached = isAttachedToWindow(itemView)
                Log.d(
                    TAG,
                    "onViewDetachedFromWindow: ${this@IFlatListItem}, " +
                            "currentSelfState=${lifecycleOwner.lifecycle.selfState}, " +
                            "currentState=${lifecycleOwner.lifecycle.currentState}, " +
                            "isAttached=${isAttached}"
                )
                if (!isAttached) {
                    val currentSelfState = lifecycleOwner.lifecycle.selfState
                    if (currentSelfState == Lifecycle.State.RESUMED) {
                        lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
                    }
                }
            } else {
                onUnBindItemView(itemView)
            }
        }

        @CallSuper
        override fun onItemInactivated() {
            Log.d(
                TAG,
                "onItemInactivated: ${this@IFlatListItem}, " +
                        "currentSelfState=${lifecycleOwner.lifecycle.selfState}, " +
                        "currentState=${lifecycleOwner.lifecycle.currentState}"
            )

            val iterator = boundItemViewRefSet.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                iterator.remove()
                next.get()?.removeOnAttachStateChangeListener(this)
            }

            lifecycleOwner.lifecycle.currentState = Lifecycle.State.CREATED
        }

        @CallSuper
        override fun onItemDetached() {
            attachedSourceToken = null
            displayedItems = null
            _attached = false
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED
            Log.d(
                TAG,
                "onItemDetached: ${this@IFlatListItem}, " +
                        "currentSelfState=${lifecycleOwner.lifecycle.selfState}," +
                        " currentState=${lifecycleOwner.lifecycle.currentState}"
            )
        }

        private fun isAttachedToWindow(detachedItemView: View? = null): Boolean {
            val iterator = boundItemViewRefSet.iterator()
            var isAttached = false
            while (iterator.hasNext()) {
                val next = iterator.next()
                val itemView = next.get()
                if (itemView == null) {
                    iterator.remove()
                } else {
                    isAttached = (itemView !== detachedItemView && itemView.isAttachedToWindow) || isAttached
                }
            }
            return isAttached
        }

        private fun getErrorMsg(
            methodName: String,
            currentSelfState: Lifecycle.State,
            targetState: Lifecycle.State
        ): String {
            return "IFlatListItem.$methodName: lifecycle state error, " +
                    "current self state must be $targetState, " +
                    "but now is $currentSelfState."
        }
    }


    private class VHReference(view: View) :
        WeakReference<View>(view) {

        private val hashCode = System.identityHashCode(view)

        override fun hashCode(): Int {
            return hashCode
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is VHReference) return false
            return get() == other.get()
        }
    }

    internal interface DataDelegate<VH : RecyclerView.ViewHolder> {
        val attached: Boolean
        fun onItemAttached()
        fun onItemActivated()
        fun updateItem(newItem: FlatListItem, payload: Any?)
        fun onItemInactivated()
        fun onItemDetached()
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


// region BoundIte

private fun View.getBoundItem(): FlatListItem? {
    return this.getTag(R.id.flat_list_bound_item_tag_id) as? FlatListItem
}

private fun View.setBoundItem(item: FlatListItem) {
    this.setTag(R.id.flat_list_bound_item_tag_id, item)
}

inline fun <reified T : FlatListItem> RecyclerView.ViewHolder.runWithListItem(crossinline block: (T) -> Unit) {
    val item = this.itemView.getTag(R.id.flat_list_bound_item_tag_id) as? T
    item?.let { block.invoke(it) }
}

// endregion