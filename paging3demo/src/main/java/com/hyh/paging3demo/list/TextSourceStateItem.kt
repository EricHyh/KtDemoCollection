package com.hyh.paging3demo.list

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.TypedViewHolderFactory
import com.hyh.list.state.AppendState
import com.hyh.list.state.AppendStateItem
import com.hyh.list.state.ItemSourceState
import com.hyh.list.state.ItemSourceStateItem

/**
 * TODO: Add Description
 *
 * @author eriche 2022/7/4
 */
class TextSourceStateItem(pagingSourceToken: Any) : ItemSourceStateItem<TextViewHolder>(pagingSourceToken) {

    companion object {
        private const val TAG = "TextAppendStateItem"
    }

    override fun bindPageState(viewHolder: TextViewHolder, state: ItemSourceState) {
        viewHolder.textView.text = when (state) {
            ItemSourceState.Loading -> {
                "加载中..."
            }
            ItemSourceState.Success -> {
                "成功"
            }
            ItemSourceState.Error -> {
                "失败了，点击重试"
            }
            ItemSourceState.Empty -> {
                "没数据"
            }
        }
        viewHolder.textView.setOnClickListener {
            if (itemSourceState == ItemSourceState.Error) {
                getRefreshActuator(viewHolder)?.invoke(false)
            }
        }
    }


    /**
     * [RecyclerView] ItemView 的类型
     */
    override fun getItemViewType(): Int {
        return 11
    }

    /**
     * 创建[RecyclerView.ViewHolder]的工厂
     *
     * @return
     */
    override fun getViewHolderFactory(): TypedViewHolderFactory<TextViewHolder> {
        return {
            TextViewHolder(
                TextView(it.context).apply {
                    setTextColor(Color.BLACK)
                    setBackgroundColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            )
        }
    }
}

class TextViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)