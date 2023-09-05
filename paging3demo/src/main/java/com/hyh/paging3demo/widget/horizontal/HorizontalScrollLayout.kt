package com.hyh.paging3demo.widget.horizontal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hyh.paging3demo.R
import com.hyh.paging3demo.widget.horizontal.internal.BaseHorizontalScrollLayout
import com.hyh.paging3demo.widget.horizontal.internal.NestedHorizontalScrollView
import com.hyh.paging3demo.widget.horizontal.internal.NestedHorizontalScrollable
import com.hyh.paging3demo.widget.horizontal.internal.Scrollable

/**
 * 水平滚动的布局
 *
 * @author eriche 2022/7/11
 */
class HorizontalScrollLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    BaseHorizontalScrollLayout(context, attrs, defStyle) {

    companion object {
        private const val TAG = "HorizontalScrollLayout"
    }

    private val fixedViewContainer: FrameLayout = FrameLayout(context)
    private val nestedHorizontalScrollView: NestedHorizontalScrollView = NestedHorizontalScrollView(context)

    private var fixedGrid: IGrid<*>? = null

    init {
        addView(fixedViewContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(nestedHorizontalScrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        initView()
    }

    override fun findFixedView(): View {
        return fixedViewContainer
    }

    override fun findScrollableView(): View {
        return nestedHorizontalScrollView
    }

    override fun asScrollable(scrollableView: View): Scrollable<*, *> {
        return NestedHorizontalScrollable(nestedHorizontalScrollView)
    }

    @Suppress("UNCHECKED_CAST")
    fun setGrid(grid: IGrid<*>, grids: List<IGrid<*>>) {
        renderFixedPositionGrid(fixedGrid = grid as IGrid<GridHolder>)
        renderScrollablePositionGrids(grids)
        syncScroll()
    }

    private val scrollableGridRecords: MutableList<PositionGridRecord> = mutableListOf()

    private fun renderScrollablePositionGrids(scrollableGrids: List<IGrid<*>>) {
        val newSize = scrollableGrids.size

        // 删除多出的Grid
        while (scrollableGridRecords.size > newSize) {
            val removed = scrollableGridRecords.removeAt(scrollableGridRecords.size - 1)
            removed.holder.view.removeFromParent()
        }
        scrollableGrids.forEachIndexed { index, grid ->
            renderScrollablePositionGrid(index, grid as IGrid<GridHolder>)
        }
    }

    private fun renderScrollablePositionGrid(index: Int, grid: IGrid<GridHolder>) {
        val size = scrollableGridRecords.size

        val record = if (size > 0 && index in 0 until size) {
            scrollableGridRecords[index]
        } else {
            null
        }

        val type = grid.gridViewType

        if (record?.type == type) {
            scrollableGridRecords[index] = PositionGridRecord(grid, type, record.holder)
            if (!grid.areContentsTheSame(record.grid)) {
                grid.render(record.holder)
            } else {
                grid.onContentsNotChanged(record.holder)
            }
            return
        }

        record?.holder?.view?.removeFromParent()
        val holder = grid.getGridHolderFactory().invoke(nestedHorizontalScrollView)
        val newRecord = PositionGridRecord(grid, type, holder)
        if (index >= size) {
            scrollableGridRecords.add(newRecord)
        } else {
            scrollableGridRecords[index] = newRecord
        }

        nestedHorizontalScrollView.addView(
            holder.view,
            index
        )

        grid.render(holder)
    }

    private fun renderFixedPositionGrid(fixedGrid: IGrid<GridHolder>) {
        val oldFixedGrid = this.fixedGrid
        val gridHolder = if (fixedViewContainer.childCount > 0) {
            val holderView = fixedViewContainer.getChildAt(0)
            holderView.getTag(R.id.grid_holder_tag) as? GridHolder
        } else {
            null
        }
        if (oldFixedGrid == null
            || oldFixedGrid.gridViewType != fixedGrid.gridViewType
            || gridHolder == null
        ) {
            fixedViewContainer.removeAllViews()
            this.fixedGrid = fixedGrid
            val holder = fixedGrid.getGridHolderFactory().invoke(fixedViewContainer)
            holder.view.setTag(R.id.grid_holder_tag, holder)
            fixedGrid.render(holder)
            fixedViewContainer.addView(holder.view)
        } else {
            this.fixedGrid = fixedGrid
            if (oldFixedGrid.gridId == fixedGrid.gridId) {
                if (!oldFixedGrid.areContentsTheSame(fixedGrid)) {
                    fixedGrid.render(gridHolder)
                }
            } else {
                fixedGrid.render(gridHolder)
            }
        }
    }

    private fun View?.removeFromParent(): Boolean {
        val parent = this?.parent
        if (parent != null && parent is ViewGroup) {
            parent.removeView(this)
            return true
        }
        return false
    }

    data class PositionGridRecord(
        val grid: IGrid<GridHolder>,
        val type: Int,
        val holder: GridHolder
    )
}


