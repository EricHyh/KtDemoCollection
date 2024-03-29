package com.hyh.toast.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.kapt_annotation.TestAnnotation

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/3/24
 */
@TestAnnotation
object DisplayUtil {
    private var windowManager: WindowManager? = null

    private fun getWindowManager(context: Context): WindowManager? {
        if (windowManager == null) {
            windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
        return windowManager
    }

    // 根据手机的分辨率将dp的单位转成px(像素)
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    // 根据手机的分辨率将px(像素)的单位转成dp
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    // 将px值转换为sp值
    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    // 将sp值转换为px值
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    // 屏幕宽度（像素）
    fun getScreenWidth(context: Context): Int {
        val metric = DisplayMetrics()
        getWindowManager(context)!!.defaultDisplay.getMetrics(metric)
        return metric.widthPixels
    }

    // 屏幕高度（像素）
    fun getScreenHeight(context: Context): Int {
        val metric = DisplayMetrics()
        getWindowManager(context)!!.defaultDisplay.getMetrics(metric)
        return metric.heightPixels
    }

    fun getScreenDensity(context: Context): Float {
        return try {
            val displayMetrics = DisplayMetrics()
            getWindowManager(context)!!.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.density
        } catch (e: Throwable) {
            e.printStackTrace()
            1.0f
        }
    }

    fun getDensityDpi(context: Context): Int {
        return try {
            val displayMetrics = DisplayMetrics()
            getWindowManager(context)!!.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.densityDpi
        } catch (e: Throwable) {
            e.printStackTrace()
            320
        }
    }
}