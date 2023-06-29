package com.hyh.paging3demo.list

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.hyh.list.adapter.MultiItemSourceAdapter
import com.hyh.page.pageContext
import com.hyh.paging3demo.R
import android.os.Build
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.Toast
import java.lang.Exception

import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hyh.list.decoration.FillBottomDecoration
import com.hyh.paging3demo.list.animator.TestSimpleItemAnimator
import com.hyh.sticky.MultiSourceStickyItemsAdapter
import com.hyh.sticky.StickyItemsLayout


class ListTestActivity : AppCompatActivity() {

    private val TAG = "ListTestActivity_"

    companion object {

    }

    val repo = NumItemSourceRepo()


    val handler = Handler()
    val refreshRunnable = object : Runnable {
        override fun run() {
            //testAdapter.refresh()
            multiSourceAdapter.refreshRepo(Unit)
            handler.postDelayed(this, 200)
        }
    }

    val multiSourceAdapter = MultiItemSourceAdapter<Unit>(this.pageContext)
    //val testAdapter = TestAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        ImmersionBar.with(this).init()

        setContentView(R.layout.activity_test_list)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = multiSourceAdapter
        recyclerView.itemAnimator = TestSimpleItemAnimator()
        recyclerView.addItemDecoration(FillBottomDecoration())


        val stickyItemsLayout = findViewById<StickyItemsLayout>(R.id.sticky_items_layout)
        stickyItemsLayout.setup(
            recyclerView, MultiSourceStickyItemsAdapter(
                multiSourceAdapter,
                1
            )
        )


        multiSourceAdapter.submitData(repo.flow)

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

        /*ListConfig.aliveItems.observeForever {
            Log.d(TAG, "aliveItems: $it")
        }*/

        //findViewById<CheckBox>(R.id.cb)
        val checkBox = findViewById<CheckBox>(R.id.cb)
        checkBox.isClickable = false
        findViewById<FrameLayout>(R.id.cb_container).setOnClickListener {
            Toast.makeText(this, "点击", Toast.LENGTH_SHORT).show()
            checkBox.isChecked = !checkBox.isChecked
        }



    }

    fun refresh(v: View) {
        multiSourceAdapter.refreshRepo(Unit)
        //multiSourceAdapter.moveGlobalItem(1, 10)
    }

    fun startRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    fun stopRefresh(v: View) {
        handler.removeCallbacks(refreshRunnable)
    }


    /**
     * 初始化状态栏和导航栏
     */
    fun setBar(window: Window) {
        //防止系统栏隐藏时内容区域大小发生变化
        var uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !com.hyh.paging3demo.utils.OSUtils.isEMUI3_x()) {
            //适配刘海屏
            //fitsNotchScreen(window)
            //初始化5.0以上，包含5.0
            uiFlags = initBarAboveLOLLIPOP(window, uiFlags)
            //android 6.0以上设置状态栏字体为暗色
            //uiFlags = setStatusBarDarkFont(uiFlags)
            //android 8.0以上设置导航栏图标为暗色
            //uiFlags = setNavigationIconDark(uiFlags)
            //适配android 11以上
            //setBarDarkFontAboveR()
        } else {
            //初始化5.0以下，4.4以上沉浸式
            initBarBelowLOLLIPOP(window)
        }
        //隐藏状态栏或者导航栏
        //uiFlags = hideBarBelowR(uiFlags)
        //应用flag
        window.decorView.setSystemUiVisibility(uiFlags)
        //适配小米和魅族状态栏黑白
        //setSpecialBarDarkMode()
        //适配android 11以上
        hideBarAboveR()
        //导航栏显示隐藏监听，目前只支持带有导航栏的华为和小米手机
    }

    private fun setBarDarkFontAboveR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //setStatusBarDarkFontAboveR()
            //setNavigationIconDarkAboveR()
        }
    }

    /**
     * 适配刘海屏
     * Fits notch screen.
     */
    private fun fitsNotchScreen(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val lp: WindowManager.LayoutParams = window.getAttributes()
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.setAttributes(lp)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 初始化android 5.0以上状态栏和导航栏
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun initBarAboveLOLLIPOP(window: Window, uiFlags: Int): Int {
        //获得默认导航栏颜色
        var uiFlags = uiFlags

        //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态栏遮住。
        uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        //Activity全屏显示，但导航栏不会被隐藏覆盖，导航栏依然可见，Activity底部布局部分会被导航栏遮住。
        //uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //判断是否存在导航栏
        //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        //需要设置这个才能设置状态栏和导航栏颜色
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //设置状态栏颜色

        return uiFlags
    }

    /**
     * 初始化android 4.4和emui3.1状态栏和导航栏
     */
    private fun initBarBelowLOLLIPOP(window: Window) {
        //透明状态栏
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //创建一个假的状态栏
        setupStatusBarView(window)
        //判断是否存在导航栏，是否禁止设置导航栏
    }

    /**
     * Sets status bar dark font.
     * 设置状态栏字体颜色，android6.0以上
     */
    private fun setStatusBarDarkFont(uiFlags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            uiFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            uiFlags
        }
    }

    /**
     * 设置导航栏图标亮色与暗色
     * Sets dark navigation icon.
     */
    private fun setNavigationIconDark(uiFlags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            uiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            uiFlags
        }
    }

    /**
     * Hide bar.
     * 隐藏或显示状态栏和导航栏。
     *
     * @param uiFlags the ui flags
     * @return the int
     */
    private fun hideBarBelowR(uiFlags: Int): Int {
        var uiFlags = uiFlags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return uiFlags
        }
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            when (mBarParams.barHide) {
                FLAG_HIDE_BAR -> uiFlags = uiFlags or (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.INVISIBLE)
                FLAG_HIDE_STATUS_BAR -> uiFlags =
                    uiFlags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.INVISIBLE)
                FLAG_HIDE_NAVIGATION_BAR -> uiFlags =
                    uiFlags or (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                FLAG_SHOW_BAR -> uiFlags = uiFlags or View.SYSTEM_UI_FLAG_VISIBLE
                else -> {
                }
            }
        }*/
        return uiFlags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun hideBarAboveR() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller: WindowInsetsController = mContentView.getWindowInsetsController()
            if (controller != null) {
                when (mBarParams.barHide) {
                    FLAG_HIDE_BAR -> {
                        controller.hide(WindowInsets.Type.statusBars())
                        controller.hide(WindowInsets.Type.navigationBars())
                    }
                    FLAG_HIDE_STATUS_BAR -> controller.hide(WindowInsets.Type.statusBars())
                    FLAG_HIDE_NAVIGATION_BAR -> controller.hide(WindowInsets.Type.navigationBars())
                    FLAG_SHOW_BAR -> {
                        controller.show(WindowInsets.Type.statusBars())
                        controller.show(WindowInsets.Type.navigationBars())
                    }
                    else -> {
                    }
                }
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }*/
    }

    /**
     * 设置一个可以自定义颜色的状态栏
     */
    private fun setupStatusBarView(window: Window) {
        /*var statusBarView: View = window.decorView.findViewById(IMMERSION_STATUS_BAR_VIEW_ID)
        if (statusBarView == null) {
            statusBarView = View(mActivity)
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                mBarConfig.getStatusBarHeight()
            )
            params.gravity = Gravity.TOP
            statusBarView.layoutParams = params
            statusBarView.visibility = View.VISIBLE
            statusBarView.id = IMMERSION_STATUS_BAR_VIEW_ID
            mDecorView.addView(statusBarView)
        }
        if (mBarParams.statusBarColorEnabled) {
            statusBarView.setBackgroundColor(
                ColorUtils.blendARGB(
                    mBarParams.statusBarColor,
                    mBarParams.statusBarColorTransform, mBarParams.statusBarAlpha
                )
            )
        } else {
            statusBarView.setBackgroundColor(
                ColorUtils.blendARGB(
                    mBarParams.statusBarColor,
                    Color.TRANSPARENT, mBarParams.statusBarAlpha
                )
            )
        }*/
    }

}