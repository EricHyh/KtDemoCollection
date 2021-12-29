package com.hyh.paging3demo.widget.horizontal

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.BuildConfig
import java.lang.ref.WeakReference

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/29
 */
class RecyclerViewScrollLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    BaseHorizontalScrollLayout<Int>(context, attrs, defStyle) {

    companion object {
        private const val TAG = "HorizontalScrollLayout"
    }

    private val fixedViewContainer: FrameLayout = FrameLayout(context)
    private val recyclerView: RecyclerView = RecyclerView(context)

    init {
        addView(fixedViewContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun findFixedView(): View = fixedViewContainer

    override fun findScrollableView(): View = recyclerView

    override fun asScrollable(scrollableView: View): Scrollable<Int> {
        return RecyclerViewScrollable(recyclerView)
    }

}


interface IGrid<Holder : GridHolder> {

    companion object {

        val EMPTY = object :
            IGrid<GridHolder> {

            override fun getViewType(): Int = IGrid::class.java.hashCode()

            override fun getGridHolderFactory(): (parent: ViewGroup) -> GridHolder {
                return {
                    object : GridHolder(View(it.context)) {}
                }
            }

            override fun getFieldId(): Int = -1

            override fun areContentsTheSame(other: IGrid<GridHolder>): Boolean {
                return true
            }

            override fun render(holder: GridHolder, showAssets: Boolean) {}
        }

        fun createEmpty(fieldId: Int): IGrid<GridHolder> {
            return object :
                IGrid<GridHolder> {

                override fun getViewType(): Int = IGrid::class.java.hashCode()

                override fun getGridHolderFactory(): (parent: ViewGroup) -> GridHolder {
                    return {
                        object : GridHolder(View(it.context)) {}
                    }
                }

                override fun getFieldId(): Int = fieldId

                override fun areContentsTheSame(other: IGrid<GridHolder>): Boolean {
                    return true
                }

                override fun render(holder: GridHolder, showAssets: Boolean) {}
            }
        }
    }

    fun getGridHolderFactory(): (parent: ViewGroup) -> Holder

    fun getViewType(): Int

    fun getFieldId(): Int

    fun areContentsTheSame(other: IGrid<Holder>) = false

    fun onContentsNotChanged(holder: Holder, showAssets: Boolean) {}

    fun render(holder: Holder, showAssets: Boolean)

}


typealias GridHolderFactory<Holder> = (parent: ViewGroup) -> Holder


abstract class GridHolder(val view: View)


class GridViewHolder(val holder: GridHolder) : RecyclerView.ViewHolder(holder.view)

class GridAdapter : RecyclerView.Adapter<GridViewHolder>() {

    companion object {
        private const val TAG = "GridAdapter"
    }

    private val viewTypeStorage = ViewTypeStorage()

    private val differ: AsyncListDiffer<IGrid<GridHolder>> =
        AsyncListDiffer<IGrid<GridHolder>>(this, object : DiffUtil.ItemCallback<IGrid<GridHolder>>() {

            override fun areItemsTheSame(oldItem: IGrid<GridHolder>, newItem: IGrid<GridHolder>): Boolean {
                return oldItem.getFieldId() == newItem.getFieldId()
            }

            override fun areContentsTheSame(oldItem: IGrid<GridHolder>, newItem: IGrid<GridHolder>): Boolean {
                return false
            }

            override fun getChangePayload(oldItem: IGrid<GridHolder>, newItem: IGrid<GridHolder>): Any? {
                return oldItem.areContentsTheSame(newItem)
            }
        })

    fun setGrids(grids: List<IGrid<GridHolder>>) {
        differ.submitList(grids)
    }

    override fun getItemViewType(position: Int): Int {
        val currentList = differ.currentList
        if (position in currentList.indices) {
            val grid = differ.currentList[position]
            val viewType = grid.getViewType()
            if (viewTypeStorage.get(viewType, false) == null) {
                viewTypeStorage.put(viewType, grid)
            }
            return viewType
        }
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val gridHolderFactory = viewTypeStorage.get(viewType)
        if (gridHolderFactory == null) {
            if (BuildConfig.DEBUG) {
                throw IllegalStateException("GridAdapter.onCreateViewHolder: gridHolderFactory can't be null, viewType = $viewType")
            } else {
                Log.e(TAG, "GridAdapter.onCreateViewHolder: gridHolderFactory can't be null, viewType = $viewType")
            }
        }
        val holder = gridHolderFactory?.invoke(parent) ?: object : GridHolder(ErrorItemView(parent.context)) {}
        return GridViewHolder(holder)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val currentList = differ.currentList
        if (position in currentList.indices) {
            val grid = differ.currentList[position]
            grid.render(holder.holder, true)
        }
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int, payloads: MutableList<Any>) {
        val currentList = differ.currentList
        if (position in currentList.indices) {
            val grid = differ.currentList[position]
            if (payloads.size > 0) {
                val areContentsTheSame = payloads[0] as? Boolean
                if (areContentsTheSame == true) {
                    grid.onContentsNotChanged(holder.holder, true)
                } else {
                    grid.render(holder.holder, true)
                }
            } else {
                grid.render(holder.holder, true)
            }
        }
    }

    private class ErrorItemView(context: Context) : View(context)

    inner class ViewTypeStorage {

        private val typeToItem: MutableMap<Int, WeakReference<IGrid<*>>?> = mutableMapOf()

        private var itemsSnapshot = GridsSnapshot()

        fun put(viewType: Int, grid: IGrid<*>) {
            typeToItem[viewType] = WeakReference(grid)
        }

        fun get(viewType: Int, findOnNull: Boolean = true): GridHolderFactory<out GridHolder>? {
            val weakReference = typeToItem[viewType]
            var item = weakReference?.get()
            if (item != null) {
                return item.getGridHolderFactory()
            }
            if (findOnNull) {
                item = findViewItemData(viewType)
            }
            if (item == null) {
                typeToItem.remove(viewType)
            } else {
                typeToItem[viewType] = WeakReference(item)
            }
            return item?.getGridHolderFactory()
        }

        private fun findViewItemData(viewType: Int): IGrid<*>? {
            val items = differ.currentList
            if (items.isNullOrEmpty()) return null
            val obtainGridsSnapshot = obtainGridsSnapshot()
            obtainGridsSnapshot.grids.addAll(items)
            var grid: IGrid<*>? = null
            kotlin.run {
                obtainGridsSnapshot.grids.forEach {
                    if (it.getViewType() == viewType) {
                        grid = it
                        return@run
                    }
                }
            }
            releaseGridsSnapshot(obtainGridsSnapshot)
            return grid
        }

        private fun obtainGridsSnapshot(): GridsSnapshot {
            return if (!itemsSnapshot.inUse) {
                itemsSnapshot
            } else {
                GridsSnapshot().apply {
                    inUse = true
                }
            }
        }

        private fun releaseGridsSnapshot(gridsSnapshot: GridsSnapshot) {
            itemsSnapshot = gridsSnapshot.apply {
                grids.clear()
                inUse = false
            }
        }
    }

    private data class GridsSnapshot(
        val grids: MutableList<IGrid<*>> = mutableListOf(),
        var inUse: Boolean = true
    )
}