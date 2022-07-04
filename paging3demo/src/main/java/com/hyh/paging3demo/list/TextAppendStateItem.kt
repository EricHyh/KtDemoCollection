package com.hyh.paging3demo.list

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.TypedViewHolderFactory
import com.hyh.list.state.AppendState
import com.hyh.list.state.AppendStateItem

/**
 * TODO: Add Description
 *
 * @author eriche 2022/7/4
 */
class TextAppendStateItem(pagingSourceToken: Any) : AppendStateItem<AppendStateTextViewHolder>(pagingSourceToken) {

    companion object {
        private const val TAG = "TextAppendStateItem"
    }

    override fun bindAppendState(viewHolder: AppendStateTextViewHolder, state: AppendState) {
        viewHolder.textView.text = when (state) {
            AppendState.Loading -> {
                "加载中..."
            }
            AppendState.Success -> {
                "成功"
            }
            AppendState.Error -> {
                "失败了，点击重试"
            }
            AppendState.NoMore -> {
                "到底了"
            }
        }
        viewHolder.textView.setOnClickListener {
            if (appendState == AppendState.Error) {
                getAppendActuator(viewHolder)?.invoke(false)
            }
        }
    }

    /**
     * [RecyclerView] ItemView 的类型
     */
    override fun getItemViewType(): Int {
        return 10
    }

    /**
     * 创建[RecyclerView.ViewHolder]的工厂
     *
     * @return
     */
    override fun getViewHolderFactory(): TypedViewHolderFactory<AppendStateTextViewHolder> {
        return {
            AppendStateTextViewHolder(
                TextView(it.context).apply {
                    setTextColor(Color.BLACK)
                    setBackgroundColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
                }
            )
        }
    }
}

class AppendStateTextViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)