package com.hyh.kt_demo.functiontest

import com.google.gson.Gson
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * [Gson]序列化实现
 *
 * @author eriche 2023/11/30
 */
abstract class GsonComplexSettingInfoStore<T> constructor()  {

    private val gson = Gson()

    fun parseConfigStr(config: String): T? {
        val parameterizedType = getParameterizedType() ?: return null
        return gson.fromJson<T>(config, parameterizedType)
    }

    fun toConfigStr(t: T): String {
        return gson.toJson(t)
    }

    private fun getParameterizedType(): Type? {
        val superclass: Type? = this.javaClass.genericSuperclass
        return if (superclass is ParameterizedType) {
            superclass.actualTypeArguments[0]
        } else {
            null
        }
    }
}