package com.hyh.list.adapter

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.FlatListItem
import com.hyh.list.IFlatListItem
import com.hyh.list.ViewHolderFactory
import com.hyh.tabs.BuildConfig
import java.lang.ref.WeakReference

/**
 * 负责将绑定UI的事件分发给[FlatListItem]
 *
 * @author eriche
 * @data 2021/6/7
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseFlatListItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "BaseFlatListItemAdapter"
        private val BOUND_POSITION_TAG_ID = ViewCompat.generateViewId()
    }

    private val viewTypeStorage: ViewTypeStorage = ViewTypeStorage()

    fun findItem(position: Int): FlatListItem? {
        val items = getFlatListItems() ?: return null
        if (position in items.indices) {
            return items[position]
        }
        return null
    }

    override fun getItemViewType(position: Int): Int {
        val items = getFlatListItems()
        if (items == null) {
            if (BuildConfig.DEBUG) {
                throw NullPointerException("ItemAdapter.getItemViewType: $position is not in itemDataList, itemDataList is null")
            } else {
                Log.e(TAG, "ItemAdapter.getItemViewType: $position is not in itemDataList, itemDataList is null")
            }
            return 0
        }
        return if (position in items.indices) {
            val itemData = items[position]
            val itemViewType = itemData.getItemViewType()
            if (viewTypeStorage.get(itemViewType, false) == null) {
                viewTypeStorage.put(itemViewType, itemData)
            }
            itemViewType
        } else {
            if (BuildConfig.DEBUG) {
                throw IndexOutOfBoundsException("ItemAdapter.getItemViewType: $position is not in itemDataList, list size is ${items.size}")
            } else {
                Log.e(TAG, "ItemAdapter.getItemViewType: $position is not in itemDataList, list size is ${items.size}")
            }
            0
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewHolderFactory = viewTypeStorage.get(viewType)
        if (viewHolderFactory == null) {
            if (BuildConfig.DEBUG) {
                throw IllegalStateException("ItemAdapter.onCreateViewHolder: viewHolderFactory can't be null, viewType = $viewType")
            } else {
                Log.e(TAG, "ItemAdapter.onCreateViewHolder: viewHolderFactory can't be null, viewType = $viewType")
            }
        }
        return viewHolderFactory?.invoke(parent) ?: object : RecyclerView.ViewHolder(ErrorItemView(parent.context)) {}
    }

    override fun getItemCount(): Int {
        return getFlatListItems()?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        dispatchBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        dispatchBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        Log.d(TAG, "onViewRecycled: $holder")

    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        Log.d(TAG, "onViewAttachedToWindow: $holder")
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "onViewDetachedFromWindow: $holder")
    }

    protected abstract fun getFlatListItems(): List<FlatListItem>?

    private fun dispatchBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (position == RecyclerView.NO_POSITION) return
        if (holder.itemView is ErrorItemView) return
        val items = getFlatListItems()
        if (items == null) {
            if (BuildConfig.DEBUG) {
                throw NullPointerException("ItemAdapter.onBindViewHolder: $position is not in itemDataList, itemDataList is null")
            } else {
                Log.e(TAG, "ItemAdapter.onBindViewHolder: $position is not in itemDataList, itemDataList is null")
            }
            return
        }
        if (position in items.indices) {
            val flatListItem = items[position] as IFlatListItem<RecyclerView.ViewHolder>
            holder.itemView.setTag(
                BOUND_POSITION_TAG_ID, BoundPosition(
                    holder.absoluteAdapterPosition,
                    position
                )
            )
            flatListItem.bindViewHolder(holder, payloads)
        } else {
            if (BuildConfig.DEBUG) {
                throw IndexOutOfBoundsException("ItemAdapter.onBindViewHolder: $position is not in itemDataList, list size is ${items.size}")
            } else {
                Log.e(TAG, "ItemAdapter.onBindViewHolder: $position is not in itemDataList, list size is ${items.size}")
            }
        }
    }

    inner class ViewTypeStorage {

        private val typeToItem: MutableMap<Int, WeakReference<FlatListItem>?> = mutableMapOf()

        fun put(viewType: Int, flatListItem: FlatListItem) {
            typeToItem[viewType] = WeakReference(flatListItem)
        }

        fun get(viewType: Int, findOnNull: Boolean = true): ViewHolderFactory? {
            val weakReference = typeToItem[viewType]
            var item = weakReference?.get()
            if (item != null) {
                return item.getViewHolderFactory()
            }
            if (findOnNull) {
                item = findViewItemData(viewType)
            }
            if (item == null) {
                typeToItem.remove(viewType)
            } else {
                typeToItem[viewType] = WeakReference(item)
            }
            return item?.getViewHolderFactory()
        }

        private fun findViewItemData(viewType: Int): FlatListItem? {
            val items = this@BaseFlatListItemAdapter.getFlatListItems()
            if (items.isNullOrEmpty()) return null
            val itemsSnapshot = mutableListOf<FlatListItem>()
            itemsSnapshot.addAll(items)
            itemsSnapshot.forEach {
                if (it.getItemViewType() == viewType) {
                    return it
                }
            }
            return null
        }
    }

    private class ErrorItemView(context: Context) : View(context)

    private data class BoundPosition(
        val globalPosition: Int,
        val localPosition: Int,
    )
}