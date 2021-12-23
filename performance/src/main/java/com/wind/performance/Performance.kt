package com.wind.performance

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewTreeObserver
import com.wind.performance.core.ActivityStack
import com.wind.performance.fps.FpsTracker
import com.wind.performance.mem.MemoryTracker
import com.wind.performance.startup.StartupTracker

/**
 * https://juejin.cn/post/6872151038305140744
 */
object Performance {
    val TAG = Performance.javaClass.simpleName
    private var mActivityLifecycleCallbacks = mutableSetOf<Application.ActivityLifecycleCallbacks>()
    var mDebug:Boolean=false
    fun install(application: Application,debug:Boolean=false) {
        mDebug=debug
        //StartupTracker.startTrack()
        //FpsTracker.startTrack()
        //MemoryTracker.startTrack()
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ActivityStack.push(activity)
                mActivityLifecycleCallbacks.forEach {
                    it.onActivityCreated(activity, savedInstanceState)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                ActivityStack.markStart()
                mActivityLifecycleCallbacks.forEach {
                    it.onActivityStarted(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                mActivityLifecycleCallbacks.forEach {
                    it.onActivityResumed(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                mActivityLifecycleCallbacks.forEach {
                    it.onActivityPaused(activity)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                ActivityStack.markStop()
                mActivityLifecycleCallbacks.forEach {
                    it.onActivityStopped(activity)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                mActivityLifecycleCallbacks.forEach {
                    it.onActivitySaveInstanceState(activity, outState)
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                mActivityLifecycleCallbacks.forEach {
                    it.onActivityDestroyed(activity)
                }
                ActivityStack.pop(activity)
            }


        })

    }



    fun addActivityLifecycleCallbacks(activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks) {
        mActivityLifecycleCallbacks.add(activityLifecycleCallbacks)
    }


}