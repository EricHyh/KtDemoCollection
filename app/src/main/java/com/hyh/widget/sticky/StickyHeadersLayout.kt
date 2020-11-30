package com.hyh.widget.sticky

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Adapter
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2020/11/30
 */
class StickyHeadersLayout : FrameLayout {

    private val mVisibleItemFinder: VisibleItemFinder = DefaultVisibleItemFinder()
    private var mRecyclerView: RecyclerView? = null
    private var mStickyHeadersAdapter: IStickyHeadersAdapter<RecyclerView.ViewHolder>? = null

    private val mOnScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            //val findFirstVisibleItemPosition = mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
            val position = mVisibleItemFinder.findFirstVisibleItemPosition(recyclerView.layoutManager)
            if (mStickyHeadersAdapter?.isStickyHeader(position) == true) {
                val viewHolder = createStickyViewHolder(recyclerView, position)
                mStickyHeadersAdapter?.onBindStickyViewHolder(viewHolder, position)

            }
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        mRecyclerView = child as RecyclerView
        mRecyclerView?.addOnScrollListener(mOnScrollListener)
    }

    @Suppress("UNCHECKED_CAST")
    fun setAdapter(adapter: IStickyHeadersAdapter<*>) {
        this.mStickyHeadersAdapter = adapter as IStickyHeadersAdapter<RecyclerView.ViewHolder>
    }

    //region private
    fun createStickyViewHolder(recyclerView: RecyclerView, position: Int): RecyclerView.ViewHolder {
        val itemViewType = recyclerView.adapter?.getItemViewType(position)
        val viewHolder = recyclerView.adapter?.onCreateViewHolder(this, itemViewType!!)
        return viewHolder!!
    }
    //endregion
}