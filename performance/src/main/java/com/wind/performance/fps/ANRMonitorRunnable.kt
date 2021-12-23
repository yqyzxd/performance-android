package com.wind.performance.fps

import android.app.Activity
import java.lang.ref.WeakReference

abstract class ANRMonitorRunnable (
    var activityRef:WeakReference<Activity>,
    var invalid:Boolean=false
        ) :Runnable{


}