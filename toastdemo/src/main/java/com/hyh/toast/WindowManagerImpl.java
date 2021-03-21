package com.hyh.toast;

import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/3/11
 */
class WindowManagerImpl implements WindowManager {


    private WindowManager mWindowManager;

    public WindowManagerImpl(WindowManager mWindowManager) {
        this.mWindowManager = mWindowManager;
    }

    @Override
    public Display getDefaultDisplay() {
        return mWindowManager.getDefaultDisplay();
    }

    @Override
    public void removeViewImmediate(View view) {
        mWindowManager.removeViewImmediate(view);
    }

    @NonNull
    @Override
    public WindowMetrics getCurrentWindowMetrics() {
        return mWindowManager.getCurrentWindowMetrics();
    }

    @NonNull
    @Override
    public WindowMetrics getMaximumWindowMetrics() {
        return mWindowManager.getMaximumWindowMetrics();
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams params) {
        if (params instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams layoutParams = (LayoutParams) params;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| LayoutParams.FLAG_FULLSCREEN;

            layoutParams.width = LayoutParams.MATCH_PARENT;
            layoutParams.height = LayoutParams.MATCH_PARENT;
        }
        mWindowManager.addView(view, params);
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (params instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams layoutParams = (LayoutParams) params;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | LayoutParams.FLAG_FULLSCREEN;


            layoutParams.width = LayoutParams.MATCH_PARENT;
            layoutParams.height = LayoutParams.MATCH_PARENT;
        }
        mWindowManager.updateViewLayout(view, params);
    }

    @Override
    public void removeView(View view) {
        mWindowManager.removeView(view);
    }
}