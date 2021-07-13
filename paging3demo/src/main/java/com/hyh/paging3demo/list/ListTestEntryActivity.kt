package com.hyh.paging3demo.list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hyh.paging3demo.R
import com.hyh.paging3demo.fragment.ProjectsFragment2
import com.hyh.paging3demo.list.fragment.TradeTabPageFragment

class ListTestEntryActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_test_entry)
    }

    fun openTradeTabPage(view: View) {
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, TradeTabPageFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun openAccountPage(view: View) {}

}