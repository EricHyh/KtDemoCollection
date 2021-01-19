package com.hyh.paging3demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.hyh.paging3demo.base.Global
import com.hyh.paging3demo.fragment.ProjectsFragment
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.internal.AtomicOp
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun openPagingSourceType(view: View) {
        openList(Global.PAGING_SOURCE_TYPE)
    }

    fun openPrevPageType(view: View) {
        openList(Global.SUPPORT_PREV_PAGE_TYPE)
    }

    fun openRemoteMediatorType(view: View) {
        openList(Global.REMOTE_MEDIATOR_TYPE)
    }

    private fun openList(type: Int) {
        Global.sourceType = type
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, ProjectsFragment::class.java, Bundle())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }
}