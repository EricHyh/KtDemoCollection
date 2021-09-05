package com.hyh.activity

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.hyh.demo.R


class AppbarLayoutTestActivity : AppCompatActivity() {

    private val mAppBarLayout: AppBarLayout by lazy {
        findViewById<AppBarLayout>(R.id.appbarlayout)
    }

    private val mTopList: RecyclerView by lazy {
        findViewById<RecyclerView>(R.id.top_list)
    }

    private val mBottomList: RecyclerView by lazy {
        findViewById<RecyclerView>(R.id.bottom_list)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appbarlayout_test)
        mTopList.adapter = TopListAdapter()
        mBottomList.adapter = BottomListAdapter()
    }

    fun getScrollOffset(view: View) {
        Toast.makeText(this, "${mAppBarLayout.top}", Toast.LENGTH_LONG).show()
    }


    var currentOffset = 0

    fun fold(view: View) {
        currentOffset = mAppBarLayout.top
        mAppBarLayout.setExpanded(false)
    }

    fun expand(view: View) {
        //mAppBarLayout.setExpanded(true)
        val behavior: AppBarLayout.Behavior = (mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams).behavior as AppBarLayout.Behavior

        val animator = ValueAnimator.ofInt(mAppBarLayout.top, currentOffset).setDuration(300L)
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            behavior.setTopAndBottomOffset(value)
        }
        animator.start()



        //behavior?.onNestedPreScroll((mAppBarLayout.parent as CoordinatorLayout), mAppBarLayout, mAppBarLayout, 0, 60, intArrayOf(0, 0))
    }

    inner class TopListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val textView = TextView(parent.context)
            textView.setTextColor(Color.BLACK)
            textView.gravity = Gravity.CENTER
            textView.setPadding(20)
            return object : RecyclerView.ViewHolder(textView) {
            }
        }

        override fun getItemCount(): Int {
            return 10
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as TextView).text = "数据：${position}"
        }
    }


    inner class BottomListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val textView = TextView(parent.context)
            textView.setTextColor(Color.BLACK)
            textView.gravity = Gravity.CENTER
            textView.setPadding(20)
            return object : RecyclerView.ViewHolder(textView) {
            }
        }

        override fun getItemCount(): Int {
            return 30
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as TextView).text = "数据：${position}"
        }
    }
}
