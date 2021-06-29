package com.hyh.paging3demo.list

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.decoration.BaseItemSourceFrameDecoration
import com.hyh.list.adapter.MultiSourceAdapter
import com.hyh.list.decoration.ItemSourceFrameDecoration
import com.hyh.page.pageContext
import com.hyh.paging3demo.R

class TestMultiTabsListActivity : AppCompatActivity() {

    val multiSourceAdapter = MultiSourceAdapter<Unit>(this.pageContext)
    //val testAdapter = TestAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_multi_tabs_list)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = multiSourceAdapter
        recyclerView.itemAnimator = null
        recyclerView.addItemDecoration(ItemSourceFrameDecoration(40, 20F, 0xFFEEEEEE.toInt()))

        Handler().postDelayed({
            multiSourceAdapter.submitData(MultiTabsItemSourceRepo().flow)
        }, 2000)


        /*ListConfig.aliveItems.observeForever {
            Log.d(TAG, "aliveItems: $it")
        }*/
    }

    fun refresh(v: View) {
        multiSourceAdapter.refreshRepo(Unit)
    }
}