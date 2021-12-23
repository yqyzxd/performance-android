package com.wind.performance.fps

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.ViewTreeObserver
import com.wind.mlog.MLog
import com.wind.performance.Performance
import com.wind.performance.Reflection
import com.wind.performance.SimpleActivityLifecycleCallbacks
import com.wind.performance.core.*
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.cos

object FpsTracker : ITracker, LooperDispatchListener() {
    const val TAG = "FpsTracker"
    const val CALLBACK_INPUT = 0
    const val CALLBACK_ANIMATION = 1
    const val CALLBACK_TRAVERSAL = 2

    private var mChoreographer: Choreographer? = null
    private val mActivityLifecycleCallbacks = object : SimpleActivityLifecycleCallbacks() {
        override fun onActivityPaused(activity: Activity) {
            pauseTrack()
        }

        override fun onActivityResumed(activity: Activity) {
            activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener(object :
                ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {

                    if (hasFocus) {
                        resumeTrack()
                        activity.window.decorView.viewTreeObserver.removeOnWindowFocusChangeListener(
                            this
                        )
                    }

                }

            })
        }
    }
    private var mChoreographerLock: Any? = null
    private var mChoreographerCallbackQueues: Array<Any>? = null
    private var mChoreographerFrameIntervalNanos: Long? = 0
    private var mChoreographerAddInputCallbackLockedMethod: Method? = null
    private var mChoreographerAddAnimationCallbackLockedMethod: Method? = null
    private var mChoreographerAddTraversalCallbackLockedMethod: Method? = null

    private var mANRHandler: Handler
    private var mHandler: Handler
    private var mFpsThread: FpsThread

    private var mStartTime: Long = 0
    private var mANRMonitorRunnable: ANRMonitorRunnable? = null
    private var mLinkedBlockingQueue: LinkedBlockingQueue<Runnable> = LinkedBlockingQueue()
    private var mOnFpsTrackListener: OnFpsTrackListener? = null

    init {
        mHandler = Handler(PerformanceHandlerThread.getLooper())
        mANRHandler = PerformanceHandlerThread.getHandler()
        mFpsThread = FpsThread(mLinkedBlockingQueue)
        mFpsThread.start()
    }

    private fun pauseTrack() {
        LooperMonitor.unregister(this)
        mHandler.removeCallbacksAndMessages(null)
        mANRHandler.removeCallbacksAndMessages(null)
        mLinkedBlockingQueue.clear()
        resetCollectItem()
        mStartTime = 0
    }

    private fun resumeTrack() {
        if (mChoreographer == null) {
            try {


                mChoreographer = Choreographer.getInstance()

                mChoreographerLock = Reflection()
                    .on(Choreographer::class.java)
                    .field("mLock")
                    .invoke(mChoreographer)
                mChoreographerCallbackQueues = Reflection()
                    .on(Choreographer::class.java)
                    .field("mCallbackQueues")
                    .invoke(mChoreographer)

                mChoreographerFrameIntervalNanos = Reflection()
                    .on(Choreographer::class.java)
                    .field("mFrameIntervalNanos")
                    .invoke(mChoreographer)
                /*
                 * NoSuchMethodException Choreographer$CallbackQueue.addCallbackLocked
                  public void addCallbackLocked(long dueTime, Object action, Object token)
                 */
                var inputCallbackQueue = mChoreographerCallbackQueues?.get(CALLBACK_INPUT)
                mChoreographerAddInputCallbackLockedMethod = Reflection()
                    .on(inputCallbackQueue!!.javaClass)
                    .getMethod(
                        "addCallbackLocked",
                        Long::class.java,
                        Object::class.java,
                        Object::class.java
                    )

                var animationCallbackQueue = mChoreographerCallbackQueues?.get(CALLBACK_ANIMATION)
                mChoreographerAddAnimationCallbackLockedMethod = Reflection()
                    .on(animationCallbackQueue!!.javaClass)
                    .getMethod(
                        "addCallbackLocked",
                        Long::class.java,
                        Object::class.java,
                        Object::class.java
                    )

                var traversalCallbackQueue = mChoreographerCallbackQueues?.get(CALLBACK_TRAVERSAL)
                mChoreographerAddTraversalCallbackLockedMethod = Reflection()
                    .on(traversalCallbackQueue!!.javaClass)
                    .getMethod(
                        "addCallbackLocked",
                        Long::class.java,
                        Object::class.java,
                        Object::class.java
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        LooperMonitor.register(this)
        addFrameCallback()

    }

    private var mInDoFrame: Boolean = false
    private fun addFrameCallback() {
        addFrameCallback(CALLBACK_INPUT, {
            mInDoFrame = true
        }, true)
    }

    private fun addFrameCallback(type: Int, callback: Runnable, isAddHeader: Boolean) {
        try {
            synchronized(mChoreographerLock!!) {
                var method: Method? = null
                when (type) {
                    CALLBACK_INPUT -> method = mChoreographerAddInputCallbackLockedMethod
                    CALLBACK_ANIMATION -> method = mChoreographerAddAnimationCallbackLockedMethod
                    CALLBACK_TRAVERSAL -> method = mChoreographerAddTraversalCallbackLockedMethod

                }

                method?.apply {
                    val dueTime = if (isAddHeader) -1 else SystemClock.uptimeMillis()
                    invoke(mChoreographerCallbackQueues!![type], dueTime, callback, null)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun startTrack() {
        Performance.addActivityLifecycleCallbacks(mActivityLifecycleCallbacks)

    }


    override fun onDispatchStart() {
        mStartTime = SystemClock.uptimeMillis()
        if (mANRMonitorRunnable == null) {
            mANRMonitorRunnable =
                object : ANRMonitorRunnable(WeakReference(ActivityStack.getTopActivity())) {
                    override fun run() {
                        if (this.activityRef != null && this.activityRef.get() != null && !this.invalid) {
                            mOnFpsTrackListener?.onANRAppear(activityRef.get()!!)
                        }
                    }

                }
        } else {
            mANRMonitorRunnable!!.activityRef =
                WeakReference(ActivityStack.getTopActivity())
        }

        mANRMonitorRunnable?.invalid = false
        mLinkedBlockingQueue.add {
            mANRHandler.removeCallbacksAndMessages(null)
            mANRHandler.postDelayed(mANRMonitorRunnable!!, 5000)
        }

    }

    override fun onDispatchEnd() {
        mANRMonitorRunnable?.invalid = true
        if (mStartTime > 0) {
            val cost = SystemClock.uptimeMillis() - mStartTime
            val isDoFrame = mInDoFrame
            mLinkedBlockingQueue.add {
                collectInfoAndDispatch(ActivityStack.getTopActivity(), cost, isDoFrame)
            }
            // println("${TAG} onDispatchEnd mInDoFrame:${mInDoFrame}")
            if (mInDoFrame) {
                addFrameCallback()
                mInDoFrame = false
            }
        }

    }

    private var mCollectItem: CollectItem = CollectItem()
    private var mHasPrint60Fps = false
    private fun collectInfoAndDispatch(activity: Activity?, cost: Long, doFrame: Boolean) {

        if (cost <= 16) {
            //16毫秒内完成正常
            mCollectItem.sumFrame++
            mCollectItem.sumCost += Math.max(16, cost)


            if (Performance.mDebug) {
                mHandler.post {
                    if (mCollectItem.sumFrame > 10) {
                        var averageFps = getAverageFps()
                        mOnFpsTrackListener?.onFpsTrack(activity!!, cost, 0, doFrame, averageFps)

                        if ((averageFps == 60L && !mHasPrint60Fps) || averageFps != 60L) {
                            if (averageFps == 60L) {
                                mHasPrint60Fps = true
                            }
                            MLog.getDefault().d(TAG, " averageFps:${averageFps} ")
                        }

                    }
                }
            }


        } else {

            mHandler.post {
                synchronized(this@FpsTracker) {
                    if (activity != null) {
                        mCollectItem.activity = activity
                        mCollectItem.sumFrame++
                        mCollectItem.sumCost += Math.max(16, cost)
                        val sumFrame = mCollectItem.sumFrame.toLong()
                        val sumCost = mCollectItem.sumCost

                        if (sumFrame > 3 && sumCost > 0) {
                            //计算fps 1s内有多少桢
                            var averageFps = getAverageFps()
                            //计算丢帧数
                            var dropFrames = Math.max(1, cost / 16 - 1)
                            mOnFpsTrackListener?.onFpsTrack(
                                activity,
                                cost,
                                dropFrames,
                                doFrame,
                                averageFps
                            )
                            if (Performance.mDebug) {
                                var sBuilder = StringBuilder(32)
                                sBuilder.append("cost:${cost}ms ")
                                    .append(" dropFrames:${dropFrames} ")
                                    .append(" averageFps:${averageFps} ")
                                MLog.getDefault().d(TAG, sBuilder.toString())
                            }

                        }
                        //   超过60桢释放
                        if (mCollectItem.sumFrame > 60) {
                            resetCollectItem()
                        }

                    }
                }
            }
        }


    }

    private fun getAverageFps(): Long {
        val sumFrame = mCollectItem.sumFrame.toLong()
        val sumCost = mCollectItem.sumCost
        var averageFps = 0L
        if (sumCost > 0) {
            averageFps = Math.min(60, sumFrame * 1000 / sumCost)
        }
        return averageFps
    }

    private fun resetCollectItem() {
        mCollectItem.sumCost = 0
        mCollectItem.sumFrame = 0
        mCollectItem.activity = null
    }


}