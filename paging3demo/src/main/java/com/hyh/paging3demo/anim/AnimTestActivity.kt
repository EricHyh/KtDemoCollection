package com.hyh.paging3demo.anim

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hyh.paging3demo.R

/**
 * TODO: Add Description
 *
 * @author eriche 2022/1/13
 */
class AnimTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AnimTestActivity"
    }


    private val rgMode by lazy {
        findViewById<RadioGroup>(R.id.rg_mode)
    }

    private var mode1Fragment: Mode1Fragment = Mode1Fragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_anim_test)
        rgMode.setOnCheckedChangeListener { _, _ ->
            switchFragment()
        }
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, mode1Fragment).commitNow()
    }

    private fun switchFragment() {
        val mode2Fragment = Mode2Fragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, mode2Fragment).commitNow()
        mode1Fragment.animOut {
            mode2Fragment.animIn()
            Handler(Looper.getMainLooper()).post {
                supportFragmentManager.beginTransaction().remove(mode1Fragment).commitNow()
            }
        }
    }
}