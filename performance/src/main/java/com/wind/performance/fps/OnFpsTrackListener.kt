package com.wind.performance.fps

import android.app.Activity

interface OnFpsTrackListener {

    fun onFpsTrack(
        activity: Activity,
        currentCostMills: Long,
        currentDropFrame: Long,
        inFrameDraw: Boolean,
        averageFps: Long
    )
    fun onANRAppear(activity: Activity)
}