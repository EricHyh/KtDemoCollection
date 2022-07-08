package com.hyh.paging3demo

import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.keyIterator
import androidx.lifecycle.lifecycleScope
import com.hyh.paging3demo.bean.ProjectRemoteKey
import com.hyh.paging3demo.db.ProjectDB
import kotlinx.coroutines.launch

/**
 * TODO: Add Description
 *
 * @author eriche 2021/11/10
 */
class SqliteTestActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SqlitTestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sqlite)
    }


    fun insert(v: View) {
        lifecycleScope.launch {
            ProjectDB.get(applicationContext).remoteKeys().insert(ProjectRemoteKey(100, -1, 1))
        }
    }


    fun query(v: View) {
        lifecycleScope.launch {
            val remoteKey = ProjectDB.get(applicationContext).remoteKeys().getRemoteKey(100)
            Log.d(TAG, "query: $remoteKey")
        }
    }
}