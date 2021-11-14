package com.hyh.paging3demo.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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