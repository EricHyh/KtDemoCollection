package com.hyh.paging3demo.list

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.adapter.MultiSourceAdapter
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListTestActivity : AppCompatActivity() {

    val handler = Handler()
    val refreshRunnable = object : Runnable {
        override fun run() {
            //testAdapter.refresh()
            multiSourceAdapter.refresh(Unit)
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

        Handler().postDelayed({
            lifecycle.coroutineScope.launch {
                NumItemSourceRepo()
                    .flow
                    .collectLatest {
                        multiSourceAdapter.submitData(it)
                        //testAdapter.refresh()
                    }
            }
        }, 2000)

        val tvTypes = findViewById<TextView>(R.id.tv_types)
        ListConfig.typesLiveData.observe(this, Observer<List<String>> { list ->
            var str = ""
            list?.forEach {
                str += it
            }
            tvTypes.text = str
        })

        val lastTvTypes = findViewById<TextView>(R.id.last_tv_types)
        ListConfig.lastTypesLiveData.observe(this, Observer<List<String>> { list ->
            var str = ""
            list?.forEach {
                str += it
            }
            lastTvTypes.text = str
        })
    }

    fun refresh(v: View) {
        multiSourceAdapter.refresh(Unit)
    }

    fun startRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    fun stopRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable)
    }
}