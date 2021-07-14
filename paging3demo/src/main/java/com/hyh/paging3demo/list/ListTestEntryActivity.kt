package com.hyh.paging3demo.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hyh.paging3demo.R
import com.hyh.paging3demo.list.fragment.AccountPageFragment
import com.hyh.paging3demo.list.fragment.TradeTabPageFragment

class ListTestEntryActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_test_entry)
    }

    fun openTradeTabPage(view: View) {
        TradeTabPageFragment.withItemAnimator = false
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, TradeTabPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun openTradeTabPageWithItemAnim(view: View) {
        TradeTabPageFragment.withItemAnimator = true
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, TradeTabPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun openAccountPage(view: View) {
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, AccountPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }


}