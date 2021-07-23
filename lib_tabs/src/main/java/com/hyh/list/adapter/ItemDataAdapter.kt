package com.hyh.list.adapter

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.*
import com.hyh.tabs.BuildConfig
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

/**
 * 负责将绑定UI的事件分发给[ItemData]
 *
 * @author eriche
 * @data 2021/6/7
 */
@Suppress("UNCHECKED_CAST")
abstract class ItemDataAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "ItemDataAdapter"
    }

    private val viewTypeStorage: ViewTypeStorage = ViewTypeStorage()

    protected abstract fun getItemDataList(): List<ItemData>?

    override fun getItemViewType(position: Int): Int {
        val itemDataList = getItemDataList()
        if (itemDataList == null) {
            if (BuildConfig.DEBUG) {
                throw NullPointerException("ItemDataAdapter.getItemViewType: $position is not in itemDataList, itemDataList is null")
            } else {
                Log.e(TAG, "ItemDataAdapter.getItemViewType: $position is not in itemDataList, itemDataList is null")
            }
            return 0
        }
        return if (position in itemDataList.indices) {
            val itemData = itemDataList[position]
            val itemViewType = itemData.getItemViewType()
            if (viewTypeStorage.get(itemViewType, false) == null) {
                viewTypeStorage.put(itemViewType, itemData)
            }
            itemViewType
        } else {
            if (BuildConfig.DEBUG) {
                throw IndexOutOfBoundsException("ItemDataAdapter.getItemViewType: $position is not in itemDataList, list size is ${itemDataList.size}")
            } else {
                Log.e(TAG, "ItemDataAdapter.getItemViewType: $position is not in itemDataList, list size is ${itemDataList.size}")
            }
            0
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewHolderFactory = viewTypeStorage.get(viewType)
        if (viewHolderFactory == null) {
            if (BuildConfig.DEBUG) {
                throw IllegalStateException("ItemDataAdapter.onCreateViewHolder: viewHolderFactory can't be null, viewType = $viewType")
            } else {
                Log.e(TAG, "ItemDataAdapter.onCreateViewHolder: viewHolderFactory can't be null, viewType = $viewType")
            }
        }
        return viewHolderFactory?.invoke(parent) ?: object : RecyclerView.ViewHolder(ErrorItemView(parent.context)) {}
    }

    override fun getItemCount(): Int {
        return getItemDataList()?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        dispatchBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        dispatchBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
    }

    private fun dispatchBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (position == RecyclerView.NO_POSITION) return
        if (holder.itemView is ErrorItemView) return
        val itemDataList = getItemDataList()
        if (itemDataList == null) {
            if (BuildConfig.DEBUG) {
                throw NullPointerException("ItemDataAdapter.onBindViewHolder: $position is not in itemDataList, itemDataList is null")
            } else {
                Log.e(TAG, "ItemDataAdapter.onBindViewHolder: $position is not in itemDataList, itemDataList is null")
            }
            return
        }
        if (position in itemDataList.indices) {
            (itemDataList[position] as IItemData<RecyclerView.ViewHolder>).bindViewHolder(holder, payloads)
        } else {
            if (BuildConfig.DEBUG) {
                throw IndexOutOfBoundsException("ItemDataAdapter.onBindViewHolder: $position is not in itemDataList, list size is ${itemDataList.size}")
            } else {
                Log.e(TAG, "ItemDataAdapter.onBindViewHolder: $position is not in itemDataList, list size is ${itemDataList.size}")
            }
        }
    }

    inner class ViewTypeStorage {

        private val typeToItemData: MutableMap<Int, WeakReference<ItemData>?> = mutableMapOf()

        fun put(viewType: Int, itemData: ItemData) {
            typeToItemData[viewType] = WeakReference(itemData)
        }

        fun get(viewType: Int, findOnNull: Boolean = true): ViewHolderFactory? {
            val weakReference = typeToItemData[viewType]
            var itemData = weakReference?.get()
            if (itemData != null) {
                return itemData.getViewHolderFactory()
            }
            if (findOnNull) {
                itemData = findViewItemData(viewType)
            }
            if (itemData == null) {
                typeToItemData.remove(viewType)
            } else {
                typeToItemData[viewType] = WeakReference(itemData)
            }
            return itemData?.getViewHolderFactory()
        }

        private fun findViewItemData(viewType: Int): ItemData? {
            val items = this@ItemDataAdapter.getItemDataList()
            if (items.isNullOrEmpty()) return null
            val itemsSnapshot = mutableListOf<ItemData>()
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
}