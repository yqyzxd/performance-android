package com.wind.performance.startup

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewTreeObserver
import com.wind.mlog.MLog
import com.wind.performance.Performance
import com.wind.performance.SimpleActivityLifecycleCallbacks
import com.wind.performance.core.ITracker

object StartupTracker :ITracker{
    private var sLanucherFlag:Int=0x01
    private var mPauseTimestamp=0L
    private var mTimeMap= mutableMapOf<Class<out Activity>,Long>()
    private var mCodeStartupDuring=0L
    private var mMainHandler:Handler
    init {
        mMainHandler= Handler(Looper.getMainLooper())
    }
    /**
     * 冷启动Activity启动耗时=contentProvide启动时间或者Application.attachBaseContent时间-前Activity 首帧可见
     * 其他Activity启动耗时 = 当前Activity 首帧可见 - 上一个Activity onPause被调用
     */
    private val mActivityLifecycleCallbacks=object :SimpleActivityLifecycleCallbacks(){
        override fun onActivityPaused(activity: Activity) {
            sLanucherFlag=0
            mPauseTimestamp =System.currentTimeMillis()
        }
        override fun onActivityResumed(activity: Activity) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                //  10 之后有改动，第一帧可见提前了 可认为onActivityResumed之后
                mMainHandler.post {
                    //<!--获取第一帧可见时间点-->
                    onActivityShown(activity)
                }
            } else {
                //首次启动
                activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener(object:
                    ViewTreeObserver.OnWindowFocusChangeListener{
                    override fun onWindowFocusChanged(focus: Boolean) {
                        if (focus) {
                            activity.getWindow().getDecorView().getViewTreeObserver().removeOnWindowFocusChangeListener(this)
                            //<!--获取第一帧可见时间点-->
                            onActivityShown(activity)

                        }
                    }

                })
            }
        }

    }

    override fun startTrack(){
        //获取启动activity
        Performance.addActivityLifecycleCallbacks(mActivityLifecycleCallbacks)

    }

    private fun onActivityShown(activity: Activity/*,startupActivityClass: Class<out Activity>*/) {
        if (sLanucherFlag != 0 ) {
            //冷启动时间
            sLanucherFlag =0
            //if (activity.javaClass == startupActivityClass) {
                mCodeStartupDuring =System.currentTimeMillis()- StartupProvider.sStartupTimestamp
                mTimeMap.put(activity.javaClass, mCodeStartupDuring)
            //}
        }else{
            var nextActivityStartTime=System.currentTimeMillis()- mPauseTimestamp
            var existDuring= mTimeMap.get(activity.javaClass)
            if (existDuring==null || nextActivityStartTime>existDuring){
                //普通activity启动时间
                mTimeMap.put(activity.javaClass,nextActivityStartTime)
            }
        }
        if (Performance.mDebug){
            persist()
        }

    }
    private var mHasPrintCodeStartup:Boolean=false
    private fun persist(){
        MLog.getDefault().d(Performance.TAG,"--------------------------------------------")
        if (!mHasPrintCodeStartup){
            mHasPrintCodeStartup=true
            MLog.getDefault().d(Performance.TAG,"mCodeStartupDuring:${mCodeStartupDuring}")
        }

        mTimeMap.forEach {
            MLog.getDefault().d(Performance.TAG,"${it.key.simpleName}:${it.value}")
        }
    }

}