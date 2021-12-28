package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R

/**
 * TODO: Add Description
 *
 * @author eriche 2021/12/28
 */
class RecyclerViewTestActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RecyclerViewTestActivity"
    }


    lateinit var recyclerView1: RecyclerView
    lateinit var recyclerView2: RecyclerView


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_recyclerview_test)
        recyclerView1 = findViewById<RecyclerView>(R.id.recycler_view1)
        recyclerView2 = findViewById<RecyclerView>(R.id.recycler_view2)
        initRecyclerView(recyclerView1)
        initRecyclerView(recyclerView2)

        val linearLayoutManager1 = recyclerView1.layoutManager as LinearLayoutManager
        val linearLayoutManager2 = recyclerView2.layoutManager as LinearLayoutManager

        recyclerView1.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val offset1 = recyclerView1.computeHorizontalScrollOffset()
            val offset2 = recyclerView2.computeHorizontalScrollOffset()
            recyclerView2.scrollBy(offset1 - offset2, 0)
        }




        recyclerView2.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val offset1 = recyclerView1.computeHorizontalScrollOffset()
                val offset2 = recyclerView2.computeHorizontalScrollOffset()
                recyclerView1.scrollBy(offset2 - offset1, 0)
            }
        })
    }


    private fun initRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val textView = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(40, 40, 40, 40)
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                }
                return object : RecyclerView.ViewHolder(textView) {}
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as? TextView)?.text = "条目：$position"
            }

            override fun getItemCount(): Int {
                return 10
            }
        }
    }

    fun recover(view: View) {
        //val offset1 = recyclerView1.computeHorizontalScrollOffset()
        recyclerView1.scrollBy(20000, 0)
    }

}