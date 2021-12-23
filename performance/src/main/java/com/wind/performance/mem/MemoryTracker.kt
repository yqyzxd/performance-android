package com.wind.performance.mem

import android.app.Activity
import com.wind.performance.Performance
import com.wind.performance.SimpleActivityLifecycleCallbacks
import com.wind.performance.core.ITracker
import java.util.*

object MemoryTracker :ITracker {


    override fun startTrack() {
        ActivityLeakTracker.startTrack()
        BitmapSizeTracker.startTrack()
    }


    fun addOnActivityLeakListener(listener: ITrackActivityListener) {
        ActivityLeakTracker.addOnActivityLeakListener(listener)
    }

    fun removeOnActivityLeakListener(listener: ITrackActivityListener) {
        ActivityLeakTracker.removeOnActivityLeakListener(listener)
    }

    fun addOnBitmapSizeListener(listener: BitmapSizeTracker.OnTrackBitmapSizeListener){
        BitmapSizeTracker.addOnBitmapSizeListener(listener)
    }
    fun removeOnBitmapSizeListener(listener: BitmapSizeTracker.OnTrackBitmapSizeListener){
        BitmapSizeTracker.removeOnBitmapSizeListener(listener)
    }
}