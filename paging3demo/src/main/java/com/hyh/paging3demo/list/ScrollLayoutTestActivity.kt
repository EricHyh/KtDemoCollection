package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import com.hyh.paging3demo.widget.horizontal.*

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/31
 */
class ScrollLayoutTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ScrollLayoutTestActivity"
    }

    private val action = Runnable{
        Log.d("XXXXX", "onCreate: ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ScrollLayoutTestAdapter()
            setBackgroundColor(0x55FF0000)

            addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))

        }.apply {
            setContentView(this)
        }

        val handler = Handler(Looper.getMainLooper())
        handler.post(action)
        handler.removeCallbacks(action)

        val testKt = TestJava()
        //testKt.add(action)
        //testKt.remove(action)
    }
}


//interface TextInterface {
//
//    fun test()
//
//}
//
//
//class TestKt() {
//
//    val list: MutableList<TextInterface> = mutableListOf()
//
//    fun add(test: TextInterface) {
//        Log.d("TestKt", "add: $test")
//        list.add(test)
//    }
//
//    fun remove(test: TextInterface) {
//        Log.d("TestKt", "remove: $test")
//        list.remove(test)
//        Log.d("TestKt", "remove: ${list.size}")
//    }
//
//}


class ScrollLayoutTestAdapter : RecyclerView.Adapter<ScrollLayoutTestHolder>() {


    private val horizontalScrollSyncHelperMap: MutableMap<Int, HorizontalScrollSyncHelper> =
        mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrollLayoutTestHolder {
        return ScrollLayoutTestHolder(
            RecyclerViewScrollLayout(parent.context).apply {
                fixedMinWidth = 200
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )
    }

    override fun getItemCount(): Int {
        return 100
    }

    val xx = HorizontalScrollSyncHelper()

    override fun onBindViewHolder(holder: ScrollLayoutTestHolder, position: Int) {
        val horizontalScrollSyncHelper = horizontalScrollSyncHelperMap.getOrPut(position / 10) {
            HorizontalScrollSyncHelper()
            //xx
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
                TextGrid(7),
//                TextGrid(8),
//                TextGrid(9),
//                TextGrid(10),
//                TextGrid(11),
//                TextGrid(12),
//                TextGrid(13),
//                TextGrid(14),
//                TextGrid(15),
//                TextGrid(16),
//                TextGrid(17),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
//                TextGrid(18),
            )
        )
    }
}


class ScrollLayoutTestHolder(val recyclerViewScrollLayout: RecyclerViewScrollLayout) :
    RecyclerView.ViewHolder(recyclerViewScrollLayout) {
}

class FixedTextGrid(
    private val gridFieldId: Int
) : IGrid<TextHolder> {


    override val gridViewType: Int
        get() = 0

    override val gridId: Int
        get() = gridFieldId


    @SuppressLint("RestrictedApi", "WrongConstant")
    override fun getGridHolderFactory(): (parent: ViewGroup) -> TextHolder {
        return {
            val textView = LayoutInflater.from(it.context).inflate(R.layout.item_text, it, false)
            TextHolder(textView as TextView)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun render(holder: TextHolder) {
        (holder.view as TextView).text = "固定的数据: $gridFieldId"
    }

}


class TextGrid(
    private val gridFieldId: Int
) : IGrid<TextHolder> {

    override val gridViewType: Int
        get() = 1

    override val gridId: Int
        get() = gridFieldId


    override fun getGridHolderFactory(): (parent: ViewGroup) -> TextHolder {
        return {
            val textView = TextView(it.context).apply {
                textSize = 20F
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    200
                )
            }
            TextHolder(textView)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun render(holder: TextHolder) {
        (holder.view as TextView).text = "数据: $gridFieldId"
        holder.view.setOnClickListener {
            Toast.makeText(holder.view.context, "数据: $gridFieldId", Toast.LENGTH_SHORT).show()
        }
    }

}

class TextHolder(view: TextView) : GridHolder(view) {

}