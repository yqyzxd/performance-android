package com.wind.performance.core

import android.util.Printer

class LooperPrinter(val mOrigin:Printer?):Printer {
    private var mChecked:Boolean=false
    private var mValid=false
    override fun println(x: String?) {
        x?.let {
            mOrigin?.println(x)
            val char=x.get(0)
            if (!mChecked){

                if (char== '>' || char == '<'){
                    mValid=true
                }
            }
            if (mValid){
                dispatch(char== '>',x)
            }
        }



    }


    private fun dispatch(begin:Boolean ,x:String){
        LooperMonitor.getListeners().forEach { listener->
            if (begin){
               if (!listener.mStartDispatched){
                   listener.dispatchStart(x)
               }
            }else{
                if (listener.mStartDispatched){
                    listener.dispatchEnd(x)
                }
            }
        }
    }
}