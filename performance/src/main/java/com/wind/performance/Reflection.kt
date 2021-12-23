package com.wind.performance

import java.lang.reflect.Method

class Reflection {

    private var mTargetClass:Class<*>?=null
    private var mMethodName:String?=null
    private var mMethodArgs= mutableListOf<Class<*>>()
    private var mFieldName:String?=null
    fun on(targetClass:Class<*>):Reflection{
        mTargetClass=targetClass
        return this
    }

    fun method(methodName:String,vararg args:Class<*>):Reflection{
        mMethodName=methodName
        mMethodArgs.clear()
        mMethodArgs.addAll(args)
        return this
    }
    fun getMethod(methodName:String,vararg args:Class<*>):Method?{
        mTargetClass?.apply {
            val method=getDeclaredMethod(methodName,*args)
            method.isAccessible=true
            return method
        }
        return null
    }

    fun field(fieldName:String):Reflection{
        mFieldName=fieldName
        return this
    }


    fun <T> invoke(target:Any?=null,ignoreError:Boolean=true,vararg args:Any):T?{


        mTargetClass?.apply {

            try {
                var result:Any?=null
                if (mMethodName!=null){
                    val method=getDeclaredMethod(mMethodName,*mMethodArgs.toTypedArray())
                    method.isAccessible=true
                    result= method.invoke(target,args)
                }else if (mFieldName !=null){
                    val field=getDeclaredField(mFieldName)
                    field.isAccessible=true
                    result= field.get(target)
                }
                return result as T?
            }catch (e:Exception){
                e.printStackTrace()
                if (!ignoreError){
                    throw e
                }
            }


        }


        return null
    }



}