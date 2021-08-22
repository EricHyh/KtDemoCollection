package com.hyh.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hyh.demo.R


class DialogTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_dialog)
    }

    fun showDialog(view: View) {
        val dialogContent = LayoutInflater.from(this).inflate(R.layout.dialog_content, null)
        val viewPager = dialogContent.findViewById<ViewPager>(R.id.view_pager)
        viewPager.adapter = DialogViewPagerAdapter()
        BottomSheetDialog(this).apply {
            setContentView(dialogContent, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1200))
            val root: View? = delegate.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            root?.let {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(root)
                //behavior.isHideable = true
                behavior.peekHeight = 1200
            }

        }.show()

        /*viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                TODO("Not yet implemented")
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                TODO("Not yet implemented")
            }

            override fun onPageSelected(position: Int) {

            }
        })*/
    }

}


class DialogViewPagerAdapter : PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return 5
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        for (i in 0 until container.childCount) {
            (container.getChildAt(i) as RecyclerView).isNestedScrollingEnabled = false
        }
        (`object` as RecyclerView).isNestedScrollingEnabled = true
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return RecyclerView(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = null
            adapter = MyListAdapter()
            container.addView(this)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView((`object` as View))
    }
}


class MyListAdapter : RecyclerView.Adapter<ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120)
                textSize = 16F
                gravity = Gravity.CENTER
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.BLACK)
            }
        )
    }

    override fun getItemCount(): Int {
        return 20
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        (holder.itemView as TextView).text = "数据:${position}"
    }

}

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)