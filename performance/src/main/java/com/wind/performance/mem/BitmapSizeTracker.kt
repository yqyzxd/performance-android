package com.wind.performance.mem

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.wind.performance.core.ITracker

object BitmapSizeTracker : ITracker {
    private var mListeners = mutableSetOf<OnTrackBitmapSizeListener>()
    override fun startTrack() {

        /* DexposedBridge.hookAllConstructors(ImageView::class.java, object: XC_MethodHook() {
             override fun afterHookedMethod(param: MethodHookParam?) {
                 super.afterHookedMethod(param)
                 DexposedBridge.findAndHookMethod(ImageView::class.java, "setImageBitmap", Bitmap::class.java, BitmapSizeHook())
             }
         })*/


    }


    fun addOnBitmapSizeListener(listener: OnTrackBitmapSizeListener) {
        mListeners.add(listener)
    }

    fun removeOnBitmapSizeListener(listener: OnTrackBitmapSizeListener) {
        mListeners.remove(listener)
    }

    /* private class BitmapSizeHook : XC_MethodHook() {
         override fun afterHookedMethod(param: MethodHookParam?) {
             super.afterHookedMethod(param)
             var imageView:ImageView?=param?.thisObject as ImageView?
             imageView?.apply {
                 checkBitmap(imageView, imageView.drawable)
             }

         }

         private fun checkBitmap(imageView: ImageView,drawable:Drawable){
             if (drawable is BitmapDrawable){

                 var bitmap=drawable.bitmap
                 bitmap?.apply {
                     var viewWidth=imageView.width
                     var viewHeight=imageView.height


                     if (viewWidth>0 && viewHeight>0){
                         check(imageView,bitmap)
                     }else{

                         imageView.viewTreeObserver.addOnPreDrawListener(object :ViewTreeObserver.OnPreDrawListener{
                             override fun onPreDraw(): Boolean {

                                 check(imageView,bitmap)
                                 imageView.viewTreeObserver.removeOnPreDrawListener(this)
                                 return true
                             }
                         })

                     }

                 }


             }
         }


         private fun check(imageView:ImageView,bitmap: Bitmap){
             var viewWidth=imageView.width
             var viewHeight=imageView.height
             if (bitmap.width>=(viewWidth.shl(1)) &&
                 bitmap.height>=(viewHeight.shl(1))){

                 //bitmap too large
                 mListeners.forEach {
                     it.onTrackBitmapSize(imageView)
                 }
             }
         }}
     }*/


    interface OnTrackBitmapSizeListener {
        fun onTrackBitmapSize(view: View)
    }
}