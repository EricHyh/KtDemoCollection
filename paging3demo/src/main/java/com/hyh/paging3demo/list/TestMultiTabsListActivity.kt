package com.hyh.paging3demo.list

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.SingleItemSourceRepository
import com.hyh.list.adapter.SourceRepoAdapter
import com.hyh.list.decoration.ItemSourceFrameDecoration
import com.hyh.page.pageContext
import com.hyh.paging3demo.R

class TestMultiTabsListActivity : AppCompatActivity() {

    val multiSourceAdapter = SourceRepoAdapter<Unit>(this.pageContext)
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
            //multiSourceAdapter.submitData(MultiTabsItemSourceRepo().flow)
            multiSourceAdapter.submitData(SingleItemSourceRepository(TestMultiTabsItemSource()).flow)
        }, 2000)


        /*ListConfig.aliveItems.observeForever {
            Log.d(TAG, "aliveItems: $it")
        }*/
    }

    fun refresh(v: View) {
        //multiSourceAdapter.refreshRepo(Unit)
        handler.post(refreshRunnable1)
    }


    val handlerThread = HandlerThread("Refresh")
    val handler by lazy {
        handlerThread.start()
        Handler(handlerThread.looper)
    }
    val refreshRunnable = object : Runnable {
        override fun run() {
            //testAdapter.refresh()
            multiSourceAdapter.refreshRepo(Unit)
            handler.post(this)
        }
    }

    var flag = true

    val refreshRunnable1 = Runnable {
        multiSourceAdapter.refreshSources(0)
    }

    fun startRefresh(v: View) {
        /*handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)*/
        Thread {
            flag = true
            while (flag) {
                handler.post(refreshRunnable1)
            }
        }.start()

    }

    fun stopRefresh(v: View) {
        //handler.removeCallbacks(refreshRunnable)
        flag = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }
}