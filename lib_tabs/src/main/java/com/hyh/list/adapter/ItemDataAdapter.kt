package com.hyh.list.adapter

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.*
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
        return getItemDataList()?.let {
            if (position in it.indices) {
                val itemData = it[position]
                val itemViewType = itemData.getItemViewType()
                if (viewTypeStorage.get(itemViewType, false) == null) {
                    viewTypeStorage.put(itemViewType, itemData)
                }
                itemViewType
            } else {
                Log.d(TAG, "getItemViewType: 0")
                0
            }
        } ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewHolderFactory = viewTypeStorage.get(viewType)
        if (viewHolderFactory == null) {
            Log.d(TAG, "onCreateViewHolder: $viewHolderFactory")
        }
        return viewHolderFactory?.invoke(parent) ?: object : RecyclerView.ViewHolder(View(parent.context)) {}
    }

    override fun getItemCount(): Int {
        return getItemDataList()?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        getItemDataList()?.let {
            if (position in it.indices) {
                (it[position] as IItemData<RecyclerView.ViewHolder>).onBindViewHolder(holder)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (position == RecyclerView.NO_POSITION) return
        getItemDataList()?.let {
            if (position in it.indices) {
                (it[position] as IItemData<RecyclerView.ViewHolder>).onBindViewHolder(holder, payloads)
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
}