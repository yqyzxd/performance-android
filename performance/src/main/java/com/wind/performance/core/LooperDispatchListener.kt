package com.wind.performance.core

abstract class LooperDispatchListener {

    var mStartDispatched:Boolean=false

    fun dispatchStart(x:String){
        mStartDispatched=true
        onDispatchStart()
    }

    fun dispatchEnd(x:String){
        mStartDispatched=false
        onDispatchEnd()
    }

    abstract fun onDispatchStart()
    abstract fun onDispatchEnd()
}