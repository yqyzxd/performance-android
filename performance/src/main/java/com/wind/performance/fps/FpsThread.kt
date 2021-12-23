package com.wind.performance.fps

import java.util.concurrent.LinkedBlockingQueue

class FpsThread(val mQueue:LinkedBlockingQueue<Runnable>) :Thread("fps_thread") {


    override fun run() {
        super.run()
        while (true){
            try {
                val runnable=mQueue.take()
                runnable.run()
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
    }

}