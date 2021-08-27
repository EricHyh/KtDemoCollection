package com.hyh.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hyh.demo.R
import java.util.HashSet
import kotlin.math.abs
import kotlin.math.roundToInt


class DialogTestActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_dialog)
    }


    var mVerticalOffset = 0

    fun showDialog(view: View) {
        val dialogContent = LayoutInflater.from(this).inflate(R.layout.layout_list, null) as CoordinatorLayout


        val appbarlayout = dialogContent.findViewById<AppBarLayout>(R.id.appbarlayout)

        val layoutParams = appbarlayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior as AppBarLayoutBehavior


        val recyclerView = dialogContent.findViewById<MyRecyclerView>(R.id.recycler_view)
        recyclerView.apply {
            recyclerView.appBarLayout = appbarlayout
            recyclerView.appBarLayoutBehavior = behavior
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = null
            adapter = MyListAdapter1()
        }


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val xappbarlayout = appbarlayout
                Log.d("TAG", "onScrollStateChanged: ")
            }
        })

        BottomSheetDialog(this).apply {
            setContentView(dialogContent, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1200))
            val root: View? = delegate.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            root?.let {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(root)
                //behavior.isHideable = true
                behavior.peekHeight = 1200
            }

        }.show()
    }
}


class MyListAdapter1 : RecyclerView.Adapter<ItemViewHolder1>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder1 {
        return ItemViewHolder1(
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

    override fun onBindViewHolder(holder: ItemViewHolder1, position: Int) {
        (holder.itemView as TextView).text = "数据:${position}"
    }

}

class ItemViewHolder1(itemView: View) : RecyclerView.ViewHolder(itemView)


class AppBarLayoutBehavior(context: Context, attrs: AttributeSet) : AppBarLayout.Behavior(context, attrs) {

    var enableNestedScroll = true

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        return enableNestedScroll && super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type)
    }
}

class MyRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {


    private var initialTouchX: Int = 0
    private var initialTouchY: Int = 0
    private var lastTouchX: Int = 0
    private var lastTouchY: Int = 0
    private val touchSlop: Int by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    private var scrollDown = false
    private var redispatchTouchEvent = false


    var initialPosition = true

    lateinit var appBarLayout: AppBarLayout
    lateinit var appBarLayoutBehavior: AppBarLayoutBehavior


    /*override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (redispatchTouchEvent) {
                    redispatchTouchEvent = false
                    scrollDown = false
                    initialPosition = appBarLayout.top == 0
                    return false
                }
                initialTouchX = ev.x.roundToInt()
                initialTouchY = ev.y.roundToInt()
                lastTouchX = initialTouchX
                lastTouchY = initialTouchY

                initialPosition = appBarLayout.top == 0
            }
            MotionEvent.ACTION_MOVE -> {
                if (scrollDown) {
                    return false
                }
                val curX = ev.x.roundToInt()
                val curY = ev.y.roundToInt()

                val tx = curX - initialTouchX
                val ty = curY - initialTouchY

                if (initialPosition && (abs(tx) > touchSlop || abs(ty) > touchSlop)) {
                    if (ty > 0) {
                        appBarLayoutBehavior.enableNestedScroll = false
                        return rootView.let {
                            scrollDown = true
                            val newEvent = MotionEvent.obtain(ev)
                            newEvent.action = MotionEvent.ACTION_DOWN
                            redispatchTouchEvent = true
                            it.dispatchTouchEvent(newEvent)
                            false
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!redispatchTouchEvent) {
                    scrollDown = false
                    appBarLayoutBehavior.enableNestedScroll = true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }*/

}

