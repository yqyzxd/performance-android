package com.wind.performance.core

import android.os.Build
import android.os.Looper
import android.os.MessageQueue
import android.util.Printer
import com.wind.performance.Reflection

object LooperMonitor : MessageQueue.IdleHandler {


    private var mPrinter: Printer? = null
    private val mLooper: Looper = Looper.getMainLooper()
    private val mListeners = mutableSetOf<LooperDispatchListener>()

    init {

        installLooperPrinter()
       // addIdleHandler()
    }

    private fun addIdleHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mLooper.queue.addIdleHandler(this)
        } else {
            //反射获取Looper内部mQueue->messageQueue
            var looper = Looper.getMainLooper()
            var queue = Reflection().on(looper.javaClass)
                .field("mQueue")
                .invoke<MessageQueue>(looper)

            queue?.addIdleHandler(this)
        }


    }

    fun getListeners(): Set<LooperDispatchListener> {
        return mListeners
    }

    fun register(listener: LooperDispatchListener) {
        mListeners.add(listener)
    }

    fun unregister(listener: LooperDispatchListener) {
        mListeners.remove(listener)
    }

    private fun installLooperPrinter() {

        //反射内部获取printer
        var origin=Reflection()
            .on(Looper::class.java)
            .field("mLogging")
            .invoke<Printer>(mLooper)

        if (origin == mPrinter && mPrinter != null) {
            return
        }
        mPrinter = LooperPrinter(origin)
        mLooper.setMessageLogging(mPrinter)
    }

    override fun queueIdle(): Boolean {


        return true
    }


}