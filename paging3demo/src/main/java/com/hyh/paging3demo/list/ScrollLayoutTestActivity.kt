package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.widget.horizontal.GridHolder
import com.hyh.paging3demo.widget.horizontal.HorizontalScrollSyncHelper
import com.hyh.paging3demo.widget.horizontal.IGrid
import com.hyh.paging3demo.widget.horizontal.RecyclerViewScrollLayout

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/31
 */
class ScrollLayoutTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ScrollLayoutTestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ScrollLayoutTestAdapter()
            setBackgroundColor(0x55FF0000)
        }.apply {
            setContentView(this)
        }
    }
}


class ScrollLayoutTestAdapter : RecyclerView.Adapter<ScrollLayoutTestHolder>() {


    private val horizontalScrollSyncHelperMap: MutableMap<Int, HorizontalScrollSyncHelper> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrollLayoutTestHolder {
        return ScrollLayoutTestHolder(
            RecyclerViewScrollLayout(parent.context).apply {
                fixedMinWidth = 200
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        )
    }

    override fun getItemCount(): Int {
        return 100
    }

    override fun onBindViewHolder(holder: ScrollLayoutTestHolder, position: Int) {
        val horizontalScrollSyncHelper = horizontalScrollSyncHelperMap.getOrPut(position / 10) {
            HorizontalScrollSyncHelper()
        }
        holder.recyclerViewScrollLayout.bindHorizontalScrollSyncHelper(horizontalScrollSyncHelper)
        holder.recyclerViewScrollLayout.setGrid(
            FixedTextGrid(0), listOf(
                TextGrid(1),
                TextGrid(2),
                TextGrid(3),
                TextGrid(4),
                TextGrid(5),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
                TextGrid(6),
            )
        )
    }
}


class ScrollLayoutTestHolder(val recyclerViewScrollLayout: RecyclerViewScrollLayout) : RecyclerView.ViewHolder(recyclerViewScrollLayout) {
}

class FixedTextGrid(
    private val gridFieldId: Int
) : IGrid<TextHolder> {

    override fun getGridHolderFactory(): (parent: ViewGroup) -> TextHolder {
        return {
            val textView = TextView(it.context).apply {
                textSize = 20F
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            TextHolder(textView)
        }
    }

    override fun getViewType(): Int {
        return 0
    }

    override fun getFieldId(): Int {
        return gridFieldId
    }

    @SuppressLint("SetTextI18n")
    override fun render(holder: TextHolder, showAssets: Boolean) {
        (holder.view as TextView).text = "固定的数据: $gridFieldId"
    }

}


class TextGrid(
    private val gridFieldId: Int
) : IGrid<TextHolder> {

    override fun getGridHolderFactory(): (parent: ViewGroup) -> TextHolder {
        return {
            val textView = TextView(it.context).apply {
                textSize = 20F
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            TextHolder(textView)
        }
    }

    override fun getViewType(): Int {
        return 1
    }

    override fun getFieldId(): Int {
        return gridFieldId
    }

    @SuppressLint("SetTextI18n")
    override fun render(holder: TextHolder, showAssets: Boolean) {
        (holder.view as TextView).text = "数据: $gridFieldId"
    }

}

class TextHolder(view: TextView) : GridHolder(view) {

}