package com.wind.performance.core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object PerformanceHandlerThread {

    private var mHandlerThread:HandlerThread
    private var mHandler:Handler
    init {
        mHandlerThread= HandlerThread("handler_thread_performance")
        mHandlerThread.start()

        mHandler=Handler(mHandlerThread.looper)
    }

    fun getLooper():Looper{
        return mHandlerThread.looper
    }
    fun getHandler():Handler{
        return mHandler
    }


}