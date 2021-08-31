package com.hyh.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hyh.demo.R


class DialogTestActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_dialog)
    }


    var mVerticalOffset = 0

    fun showDialog(view: View) {
        val dialogContent = LayoutInflater.from(this).inflate(R.layout.layout_list, null) as CoordinatorLayout


        val appbarlayout = dialogContent.findViewById<AppBarLayout>(R.id.appbarlayout)


        val recyclerView = dialogContent.findViewById<MyRecyclerView>(R.id.recycler_view)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = null
            adapter = MyListAdapter1()
        }

        dialogContent.findViewById<View>(R.id.view_header).setOnClickListener {
            Toast.makeText(dialogContent.context, "点击", Toast.LENGTH_SHORT).show()
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

                /*val params = root.layoutParams as CoordinatorLayout.LayoutParams
                val behavior = BottomSheetBehavior<View>(root.context, null)
                params.behavior = behavior*/

                //behavior.isHideable = true
                behavior.peekHeight = 1200
            }

            setCanceledOnTouchOutside(false)

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


    private var parentCoordinatorLayout: CoordinatorLayout? = null
    private var bottomSheet: View? = null
    private var parentBehavior: CoordinatorLayout.Behavior<View>? = null

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        if (!target.canScrollVertically(-1) && child.top == 0) {
            val bottomSheet = parent.rootView.findViewById<View>(R.id.design_bottom_sheet)
            val layoutParams = bottomSheet.layoutParams as CoordinatorLayout.LayoutParams
            this.bottomSheet = bottomSheet
            parentBehavior = layoutParams.behavior
            parentCoordinatorLayout = bottomSheet.parent as CoordinatorLayout
            parentBehavior?.onStartNestedScroll(parentCoordinatorLayout!!, bottomSheet, bottomSheet, target, nestedScrollAxes, type)
        }
        return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        val bottomSheet = this.bottomSheet
        if (bottomSheet == null || bottomSheet.top + bottomSheet.height == parentCoordinatorLayout?.height ?: 0) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        }
        if (child.top == 0) {
            val newConsumed = IntArray(2)
            parentBehavior?.onNestedPreScroll(parentCoordinatorLayout!!, bottomSheet!!, target, dx, dy, newConsumed, type)
            consumed[0] += newConsumed[0]
            consumed[1] += newConsumed[1]
        }
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return child.top == 0 && parentBehavior?.onNestedPreFling(
            parentCoordinatorLayout!!,
            bottomSheet!!,
            target,
            velocityX,
            velocityY
        ) ?: false || super.onNestedPreFling(
            coordinatorLayout,
            child,
            target,
            velocityX,
            velocityY
        )
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, abl: AppBarLayout, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, abl, target, type)
        parentBehavior?.onStopNestedScroll(parentCoordinatorLayout!!, bottomSheet!!, target, type)
        this.parentBehavior = null
        this.bottomSheet = null
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (child.top == 0) {
            parentBehavior?.onNestedScroll(parentCoordinatorLayout!!, bottomSheet!!, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        }
    }

    override fun onNestedScrollAccepted(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes, type)
        parentBehavior?.onNestedScrollAccepted(parentCoordinatorLayout!!, bottomSheet!!, bottomSheet!!, target, axes, type)
    }

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }
}

class MyRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs)


class MyDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<MyRecyclerView>(R.id.recycler_view)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = null
            adapter = MyListAdapter1()
        }
    }
}