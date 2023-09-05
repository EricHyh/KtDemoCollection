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
import com.hyh.paging3demo.widget.horizontal.internal.BaseHorizontalScrollLayout
import com.hyh.paging3demo.widget.horizontal.internal.RecyclerViewScrollable
import com.hyh.paging3demo.widget.horizontal.internal.Scrollable
import com.hyh.paging3demo.widget.horizontal.internal.SyncHorizontalScrollRecyclerView
import java.lang.ref.WeakReference

/**
 * 基于RecyclerView实现的水平滑动控件
 *
 * @author eriche 2021/12/29
 */
class RecyclerViewScrollLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : BaseHorizontalScrollLayout(context, attrs, defStyle) {

    companion object {
        private const val TAG = "RecyclerViewScrollLayout"
    }

    private val fixedViewContainer: FrameLayout = FrameLayout(context)
    private val recyclerView: SyncHorizontalScrollRecyclerView =
        SyncHorizontalScrollRecyclerView(context)

    private val recyclerViewScrollable: RecyclerViewScrollable

    private val gridAdapter: GridAdapter = GridAdapter()

    init {
        addView(
            fixedViewContainer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = gridAdapter
        recyclerViewScrollable = RecyclerViewScrollable(recyclerView)
        initView()

        recyclerView.scrollListener = { data ->
            data.recyclerView.scrollState.let { scrollState ->
                when (scrollState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        notifyScrollingEvent(
                            recyclerViewScrollable.getScrollingData(data.dx),
                            recyclerViewScrollable.getScrolledData()
                        )
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        notifySettlingEvent(
                            recyclerViewScrollable.getScrollingData(data.dx),
                            recyclerViewScrollable.getScrolledData()
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    override fun findFixedView(): View = fixedViewContainer

    override fun findScrollableView(): View = recyclerView

    override fun asScrollable(scrollableView: View): Scrollable<*, *> {
        return recyclerViewScrollable
    }

    @Suppress("UNCHECKED_CAST")
    fun setGrid(grid: IGrid<*>, grids: List<IGrid<*>>) {
        renderFixedPositionGrid(grid = grid as IGrid<GridHolder>)
        gridAdapter.setGrids(grids)
        syncScroll()
    }

    private fun renderFixedPositionGrid(grid: IGrid<GridHolder>) {
        fixedViewContainer.removeAllViews()
        val holder = grid.getGridHolderFactory().invoke(fixedViewContainer)
        fixedViewContainer.addView(holder.view)
        grid.render(holder)
    }
}


interface IGrid<Holder : GridHolder> {

    companion object {

        val EMPTY = object :
            IGrid<GridHolder> {

            override val gridId: Int = -1

            override val gridViewType: Int = IGrid::class.java.hashCode()

            override fun getGridHolderFactory(): (parent: ViewGroup) -> GridHolder {
                return {
                    object : GridHolder(View(it.context)) {}
                }
            }

            override fun areContentsTheSame(other: IGrid<*>): Boolean {
                return true
            }

            override fun render(holder: GridHolder) {}
        }

        fun createEmpty(fieldId: Int): IGrid<GridHolder> {
            return object :
                IGrid<GridHolder> {

                override val gridId: Int = fieldId

                override val gridViewType: Int = IGrid::class.java.hashCode()

                override fun getGridHolderFactory(): (parent: ViewGroup) -> GridHolder {
                    return {
                        object : GridHolder(View(it.context)) {}
                    }
                }

                override fun areContentsTheSame(other: IGrid<*>): Boolean {
                    return true
                }

                override fun render(holder: GridHolder) {}
            }
        }
    }

    val gridId: Int

    val gridViewType: Int

    fun getGridHolderFactory(): (parent: ViewGroup) -> Holder

    fun areContentsTheSame(other: IGrid<*>) = false

    fun onContentsNotChanged(holder: Holder) {}

    fun render(holder: Holder)

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
        AsyncListDiffer(this, object : DiffUtil.ItemCallback<IGrid<GridHolder>>() {

            override fun areItemsTheSame(
                oldItem: IGrid<GridHolder>,
                newItem: IGrid<GridHolder>
            ): Boolean {
                return oldItem.gridId == newItem.gridId
            }

            override fun areContentsTheSame(
                oldItem: IGrid<GridHolder>,
                newItem: IGrid<GridHolder>
            ): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }

            override fun getChangePayload(
                oldItem: IGrid<GridHolder>,
                newItem: IGrid<GridHolder>
            ): Any? {
                return null
            }
        })

    @Suppress("UNCHECKED_CAST")
    fun setGrids(grids: List<IGrid<*>>) {
        differ.submitList(grids as List<IGrid<GridHolder>>)
    }

    override fun getItemViewType(position: Int): Int {
        val currentList = differ.currentList
        if (position in currentList.indices) {
            val grid = differ.currentList[position]
            val viewType = grid.gridViewType
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
                Log.e(
                    TAG,
                    "GridAdapter.onCreateViewHolder: gridHolderFactory can't be null, viewType = $viewType"
                )
            }
        }
        val holder = gridHolderFactory?.invoke(parent) ?: object :
            GridHolder(ErrorItemView(parent.context)) {}
        return GridViewHolder(holder)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val currentList = differ.currentList
        if (position in currentList.indices) {
            val grid = differ.currentList[position]
            grid.render(holder.holder)
        }
    }

    override fun onBindViewHolder(
        holder: GridViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val currentList = differ.currentList
        if (position in currentList.indices) {
            val grid = differ.currentList[position]
            if (payloads.size > 0) {
                val areContentsTheSame = payloads[0] as? Boolean
                if (areContentsTheSame == true) {
                    grid.onContentsNotChanged(holder.holder)
                } else {
                    grid.render(holder.holder)
                }
            } else {
                grid.render(holder.holder)
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
            if (items.isEmpty()) return null
            val obtainGridsSnapshot = obtainGridsSnapshot()
            obtainGridsSnapshot.grids.addAll(items)
            var grid: IGrid<*>? = null
            kotlin.run {
                obtainGridsSnapshot.grids.forEach {
                    if (it.gridViewType == viewType) {
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