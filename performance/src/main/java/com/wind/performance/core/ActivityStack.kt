package com.wind.performance.core

import android.app.Activity

object ActivityStack {
    private var mActivities= mutableListOf<Activity>()

    @Volatile
    private var mCurrentState:Int=0

    fun push(activity: Activity){
        mActivities.add(0,activity)
    }

    fun pop(activity: Activity){
        mActivities.remove(activity)
    }

    fun getTopActivity():Activity?{
        return if (mActivities.size>0) mActivities.get(0) else null
    }

    fun markStart(){
        mCurrentState++
    }

    fun markStop(){
        mCurrentState--
    }

    fun inBackground():Boolean{
        return mCurrentState == 0
    }

}