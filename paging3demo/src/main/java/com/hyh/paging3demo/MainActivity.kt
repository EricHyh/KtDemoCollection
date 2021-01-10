package com.hyh.paging3demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.hyh.paging3demo.fragment.ProjectsFragment
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.internal.AtomicOp
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, ProjectsFragment())
            .commitAllowingStateLoss()
    }
}