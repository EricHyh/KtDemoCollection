package com.hyh.list.adapter

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.core.util.set
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hyh.coroutine.SingleRunner
import com.hyh.list.IItemData
import com.hyh.list.ItemData
import com.hyh.list.SourceLoadState
import com.hyh.list.ViewHolderFactory
import com.hyh.list.internal.SourceData
import com.hyh.list.internal.SourceEvent
import com.hyh.list.internal.UiReceiverForSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext


interface ISourceAdapter {

    fun getItemCount()

    fun getItemId(position: Int): Long

    fun getItemViewType(position: Int): Int

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)

    fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver)

    fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver)

    fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder)

    fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder)

    fun onFailedToRecycleView(holder: RecyclerView.ViewHolder)

    fun onViewRecycled(holder: RecyclerView.ViewHolder)

    fun onAttachedToRecyclerView(recyclerView: RecyclerView)

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView)

    suspend fun submitData(data: SourceData<out Any>)

    fun refresh(param: Any)

}


/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/6/7
 */
@Suppress("UNCHECKED_CAST")
class SourceAdapter(
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "SourceAdapter"
    }

    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val collectFromRunner = SingleRunner()
    private val viewTypeStorage: ViewTypeStorage = ViewTypeStorage()
    private var receiver: UiReceiverForSource<Any>? = null
    private var items: List<ItemData>? = null

    private val _loadStateFlow: MutableStateFlow<SourceLoadState> = MutableStateFlow(SourceLoadState.Initial)

    override fun getItemViewType(position: Int): Int {
        return items?.let {
            if (position in it.indices) {
                val itemData = it[position]
                val itemViewType = itemData.getItemViewType()
                if (viewTypeStorage.get(itemViewType) == null) {
                    val viewHolderFactory = itemData.getViewHolderFactory()
                    viewTypeStorage.put(itemViewType, viewHolderFactory)
                }
                itemViewType
            } else {
                0
            }
        } ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) return object : RecyclerView.ViewHolder(View(parent.context)) {}
        return viewTypeStorage.get(viewType)!!.invoke(parent)
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == RecyclerView.NO_POSITION) return
        items?.let {
            if (position in it.indices) {
                (it[position] as IItemData<RecyclerView.ViewHolder>).onBindViewHolder(holder)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (position == RecyclerView.NO_POSITION) return
        items?.let {
            if (position in it.indices) {
                (it[position] as IItemData<RecyclerView.ViewHolder>).onBindViewHolder(holder, payloads)
            }
        }
    }

    suspend fun submitData(data: SourceData<out Any>) {
        collectFromRunner.runInIsolation {
            receiver = data.lazyReceiver.value as UiReceiverForSource<Any>
            data.lazyFlow.value.collect { event ->
                withContext(mainDispatcher) {
                    when (event) {
                        is SourceEvent.PreShowing -> {
                            val oldItems = items
                            val newItems = event.items
                            if (oldItems.isNullOrEmpty() || newItems.isNullOrEmpty()) {
                                items = newItems
                                viewTypeStorage.clear()
                                notifyDataSetChanged()
                            } else {
                                val diffResult = withContext(workerDispatcher) {
                                    DiffUtil.calculateDiff(DiffUtilCallback(oldItems, newItems))
                                }
                                items = newItems
                                viewTypeStorage.clear()
                                diffResult.dispatchUpdatesTo(this@SourceAdapter)
                            }
                            _loadStateFlow.value = SourceLoadState.PreShow(newItems.size)
                        }
                        is SourceEvent.Loading -> {
                            _loadStateFlow.value = SourceLoadState.Loading
                        }
                        is SourceEvent.Error -> {
                            _loadStateFlow.value = SourceLoadState.Error(event.error, event.preShowing)
                        }
                        is SourceEvent.Success -> {
                            val oldItems = items
                            val newItems = event.items
                            if (oldItems.isNullOrEmpty() || newItems.isNullOrEmpty()) {
                                items = newItems
                                notifyDataSetChanged()
                            } else {
                                val diffResult = withContext(workerDispatcher) {
                                    DiffUtil.calculateDiff(DiffUtilCallback(oldItems, newItems))
                                }
                                items = newItems
                                diffResult.dispatchUpdatesTo(this@SourceAdapter)
                            }
                            _loadStateFlow.value = SourceLoadState.Success(newItems.size)
                        }
                    }
                }
            }
        }
    }

    fun refresh(param: Any) {
        receiver?.refresh(param)
    }

    class ViewTypeStorage {

        private val viewHolderFactoryMap: SparseArray<ViewHolderFactory> = SparseArray()

        fun put(viewType: Int, viewHolderFactory: ViewHolderFactory) {
            if (viewHolderFactoryMap[viewType] != null) {
                return
            }
            viewHolderFactoryMap[viewType] = viewHolderFactory
        }

        fun get(viewType: Int): ViewHolderFactory? {
            return viewHolderFactoryMap[viewType]
        }

        fun clear() {
            viewHolderFactoryMap.clear()
        }
    }

    class DiffUtilCallback(private val oldItems: List<ItemData>, private val newItems: List<ItemData>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            return newItems.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val (oldItemData, newItemData) = getItemData(oldItemPosition, newItemPosition)
            if (oldItemData == null && newItemData == null) return true
            if (oldItemData == null || newItemData == null) return false
            return newItemData.areItemsTheSame(oldItemData)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val (oldItemData, newItemData) = getItemData(oldItemPosition, newItemPosition)
            if (oldItemData == null && newItemData == null) return true
            if (oldItemData == null || newItemData == null) return false
            return newItemData.areContentsTheSame(oldItemData)
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val (oldItemData, newItemData) = getItemData(oldItemPosition, newItemPosition)
            if (oldItemData == null && newItemData == null) return null
            if (oldItemData == null || newItemData == null) return null
            return newItemData.getChangePayload(oldItemData)
        }

        private fun getItemData(oldItemPosition: Int, newItemPosition: Int): Pair<ItemData?, ItemData?> {
            val oldItemData = if (oldItemPosition in oldItems.indices) {
                oldItems[oldItemPosition]
            } else {
                null
            }
            val newItemData = if (newItemPosition in newItems.indices) {
                newItems[newItemPosition]
            } else {
                null
            }
            return Pair(oldItemData, newItemData)
        }
    }
}