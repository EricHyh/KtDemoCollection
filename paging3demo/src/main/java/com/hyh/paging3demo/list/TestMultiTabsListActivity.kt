package com.hyh.paging3demo.list

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.SingleItemSourceRepo
import com.hyh.list.adapter.MultiItemSourceAdapter
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import com.hyh.paging3demo.list.animator.TestSimpleItemAnimator

class TestMultiTabsListActivity : AppCompatActivity() {

    val multiSourceAdapter = MultiItemSourceAdapter<Unit>(this.pageContext)
    //val testAdapter = TestAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_multi_tabs_list)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = multiSourceAdapter
        recyclerView.itemAnimator = TestSimpleItemAnimator()



        Handler().postDelayed({
            //multiSourceAdapter.submitData(MultiTabsItemSourceRepo().flow)
            multiSourceAdapter.submitData(SingleItemSourceRepo(TestMultiContentItemSource("")).flow)
        }, 2000)


        /*ListConfig.aliveItems.observeForever {
            Log.d(TAG, "aliveItems: $it")
        }*/
    }

    fun refresh(v: View) {
        multiSourceAdapter.refreshRepo(Unit)
        //handler.post(refreshRunnable1)
    }

    private val TAG = "TestMultiTabsList"

    val handlerThread = HandlerThread("Refresh")
    val handler by lazy {
        handlerThread.start()
        Handler(handlerThread.looper)
    }
    val refreshRunnable = object : Runnable {
        override fun run() {
            //testAdapter.refresh()
            multiSourceAdapter.refreshRepo(Unit)
            //Log.d(TAG, "refreshRunnable run: ")
            if (flag) {
                handler.post(this)
            }
        }
    }

    var flag = false

    val refreshRunnable1 = Runnable {
        multiSourceAdapter.refreshSources(0)
    }

    val refreshRunnable11 = object : Runnable {

        override fun run() {
            multiSourceAdapter.refreshSources(0)
            if (flag) {
                handler.post(this)
            }
        }
    }

    fun startRefresh(v: View) {
        flag = true
        handler.removeCallbacks(refreshRunnable11)
        handler.post(refreshRunnable11)
        /*Thread {
            flag = true
            while (flag) {
                handler.post(refreshRunnable1)
            }
        }.start()*/

    }

    fun stopRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable11)
        flag = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }
}