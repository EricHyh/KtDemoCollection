package com.hyh.paging3demo.list

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.ItemSourceDecoration
import com.hyh.list.SingleItemSourceRepository
import com.hyh.list.adapter.MultiSourceAdapter
import com.hyh.page.pageContext
import com.hyh.paging3demo.R

class TestMultiTabsListActivity : AppCompatActivity() {

    private val TAG = "ListTestActivity_"

    val handler = Handler()
    val refreshRunnable = object : Runnable {
        override fun run() {
            //testAdapter.refresh()
            multiSourceAdapter.refreshRepo(Unit)
            handler.post(this)
        }
    }

    val multiSourceAdapter = MultiSourceAdapter<Unit>(this.pageContext)
    //val testAdapter = TestAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_list)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = multiSourceAdapter

        recyclerView.addItemDecoration(ItemSourceDecoration())

        Handler().postDelayed({
            multiSourceAdapter.submitData(SingleItemSourceRepository(TestMultiTabsItemSource()).flow)
        }, 2000)


        /*ListConfig.aliveItems.observeForever {
            Log.d(TAG, "aliveItems: $it")
        }*/
    }

    fun refresh(v: View) {
        multiSourceAdapter.refreshRepo(Unit)
    }

    fun startRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    fun stopRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable)
    }
}