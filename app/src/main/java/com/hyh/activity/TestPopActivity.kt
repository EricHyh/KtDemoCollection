package com.hyh.activity

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hyh.demo.R

/**
 * TODO: Add Description
 *
 * @author eriche 2021/8/10
 */
class TestPopActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TestPopActivity_"
    }

    private fun initWindow() {
        val window = window
        // 使用全屏
        val uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = uiOptions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_pop)

        val view = findViewById<View>(R.id.edit_text)

        val contentView = findViewById<View>(android.R.id.content)

        val testPopActivity = this

        Handler().postDelayed(
            {
                Log.d(TAG, "onCreate: $testPopActivity")

                val array1 = intArrayOf(0, 0)
                contentView.getLocationInWindow(array1)
                Log.d(TAG, "onCreate: array1 = ${array1[0]} - ${array1[1]}")


                val rect1 = Rect()
                contentView.getGlobalVisibleRect(rect1)
                Log.d(TAG, "onCreate: rect1 = $rect1")
            }, 1000
        )


        Handler().postDelayed(
            {
                Log.d(TAG, "onCreate: $testPopActivity")

                val array2 = intArrayOf(0, 0)
                contentView.getLocationInWindow(array2)
                Log.d(TAG, "onCreate: array2 = ${array2[0]} - ${array2[1]}")

                val rect2 = Rect()
                contentView.getGlobalVisibleRect(rect2)
                Log.d(TAG, "onCreate: rect2 = $rect2")


            }, 5000
        )


        val layoutInflater = LayoutInflater.from(this).cloneInContext(this)


    }


    fun showPop1(view: View) {
        /*val popview = TextView(view.context).apply {
            setBackgroundColor(Color.RED)
            layoutParams = ViewGroup.LayoutParams(400, 200)
            gravity = Gravity.CENTER
            text = "弹窗内容"
            setTextColor(Color.WHITE)
        }

        val popupWindow = PopupWindow(popview, 400, 200).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isFocusable = true
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        }

        popupWindow.update()
        popupWindow.showAsDropDown(view, 0, calculateOffsetY(view))*/

        for (index in 0..100) {
            Thread {
                val inflate = layoutInflater.inflate(R.layout.account_pop, null)
                Log.d(TAG, "showPop1: $index -- $inflate")
            }.start()
        }
    }

    fun showPop2(view: View) {
        val popview = TextView(view.context).apply {
            setBackgroundColor(Color.RED)
            layoutParams = ViewGroup.LayoutParams(400, 200)
            gravity = Gravity.CENTER
            text = "弹窗内容"
            setTextColor(Color.WHITE)
        }

        val popupWindow = PopupWindow(popview, 400, 200).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isFocusable = true
            isOutsideTouchable = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        }

        popupWindow.update()
        popupWindow.showAsDropDown(view, 0, calculateOffsetY(view))
    }

    /**
     * 计算由于键盘弹出导致的布局偏移
     */
    private fun calculateOffsetY(view: View): Int {
        val rootView = view.rootView
        val array = intArrayOf(0, 0)
        rootView.getLocationInWindow(array)
        return array[1]
    }
}