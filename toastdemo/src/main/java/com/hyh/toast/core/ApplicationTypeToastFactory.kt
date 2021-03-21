package com.hyh.toast.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import java.lang.ref.WeakReference

object ApplicationTypeToastFactory : ToastFactory {

    private lateinit var mApplication: Application

    private val mActivityLifecycleCallbacks = InnerActivityLifecycleCallbacks()
    private var mTopActivityRef: WeakReference<Activity>? = null

    override fun init(context: Context) {
        this.mApplication = context.applicationContext as Application
        mApplication.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
    }

    override fun create(): IToast {
        val windowManager = getWindowManager() ?: return EmptyToast()
        return ApplicationTypeToast(windowManager)
    }

    private fun getWindowManager(): WindowManager? {
        return mTopActivityRef?.get()?.windowManager
    }

    class InnerActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            mTopActivityRef = WeakReference(activity)
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            mTopActivityRef?.let {
                if (it.get() == activity) {
                    it.clear()
                }
            }
        }
    }
}