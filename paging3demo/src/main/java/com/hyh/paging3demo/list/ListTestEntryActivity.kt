package com.hyh.paging3demo.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import com.hyh.paging3demo.list.fragment.AccountPageFragment
import com.hyh.paging3demo.list.fragment.TTTFragment
import com.hyh.paging3demo.list.fragment.TradeTabPageFragment
import com.hyh.paging3demo.widget.TestFrameLayout

class ListTestEntryActivity : AppCompatActivity() {

    val testFrameLayout: TestFrameLayout by lazy {
        findViewById<TestFrameLayout>(R.id.test_frame_layout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_test_entry)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
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

    fun openTradeTabPage(view: View) {
        testFrameLayout.requestLayout()
        /*TradeTabPageFragment.withItemAnimator = false
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, TradeTabPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()*/
    }

    fun openTradeTabPageWithItemAnim(view: View) {
        testFrameLayout.requestLayout()
        TradeTabPageFragment.withItemAnimator = true
        /*supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, TradeTabPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()*/
    }

    fun openAccountPage(view: View) {
        testFrameLayout.requestLayout()
        /*supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, AccountPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()*/
    }


}