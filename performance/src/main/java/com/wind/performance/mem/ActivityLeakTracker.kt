package com.wind.performance.mem

import android.app.Activity
import android.os.Handler
import android.os.SystemClock
import com.wind.mlog.MLog
import com.wind.performance.Performance
import com.wind.performance.SimpleActivityLifecycleCallbacks
import com.wind.performance.core.ITracker
import com.wind.performance.core.PerformanceHandlerThread
import java.util.*
import java.util.Map
import java.util.Set

object ActivityLeakTracker : ITracker {

    private var mActivityWeakMap: WeakHashMap<Activity, String> = WeakHashMap()
    private var mHandler: Handler = PerformanceHandlerThread.getHandler()
    private var mListeners = mutableSetOf<ITrackActivityListener>()
    private val mActivityLifecycleCallbacks = object : SimpleActivityLifecycleCallbacks() {

        override fun onActivityDestroyed(activity: Activity) {
            super.onActivityDestroyed(activity)

            mActivityWeakMap.put(activity, activity.javaClass.simpleName)


            //检查是否有泄露
            mHandler.postDelayed(
                {
                    checkLeak()
                }, 100L
            )
        }

        /**
         * 如果在线上使用应该将检测泄露逻辑放在应用退到后台之后。 测试环境则放置在 activity destroy之后检测
         */
        override fun onActivityStopped(activity: Activity) {
            super.onActivityStopped(activity)


        }

    }

    private fun checkLeak() {
        Runtime.getRuntime().gc()
       // SystemClock.sleep(100)
        System.runFinalization()

        var entries: MutableSet<MutableMap.MutableEntry<Activity, String>> =
            mActivityWeakMap.entries
        var leaks = mutableSetOf<String>()
        entries.forEach { entry ->
            var name = entry.key.javaClass.canonicalName
            //leak
            leaks.add(name)
        }

        MLog.getDefault().e("Performance","${leaks}")
        mListeners.forEach {
            it.onActivityLeak(leaks)
        }
    }

    override fun startTrack() {
        Performance.addActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
    }


    fun addOnActivityLeakListener(listener: ITrackActivityListener) {
        mListeners.add(listener)
    }

    fun removeOnActivityLeakListener(listener: ITrackActivityListener) {
        mListeners.remove(listener)
    }
}

interface ITrackActivityListener {
    fun onActivityLeak(activitys: kotlin.collections.Set<String>)
}