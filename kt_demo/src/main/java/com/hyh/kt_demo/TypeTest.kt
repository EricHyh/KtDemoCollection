package com.hyh.kt_demo

import java.lang.String
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * TODO: Add Description
 *
 * @author eriche 2021/8/18
 */

fun main() {


    val xx =
        "283445307142697904:9|283445320027599792:11|283726802294245040:11|281756462277401264:3|281756457682433968:2|281756461977401264:8|281756457982433968:7|281756462077401264:1|281756457782433968:3|281756455982434268:12|281756455982446768:11|281756457882433968:6|281756462177401264:0|281756460277401264:1|281756468867335856:5|281756473162303152:5|"


    val xxx: java.lang.String = String("283445307142697904:9|283445320027599792:11")

    val split = xxx.split("|")
    val split1 = xxx.split("\\|")

    ChildD().test()
    ChildA().test()
    ChildB().test()
    ChildC().test()
}


open class Holder

class Holder1 : Holder()
class Holder2 : Holder()
class Holder3 : Holder()

abstract class Base<H : Holder> {


    fun test() {
        println()
    }

    private fun findViewHoldType(): Type? {
        return findViewHoldType(this.javaClass.genericSuperclass, this.javaClass.superclass)
    }

    private fun findViewHoldType(type: Type?, cls: Class<*>?): Type? {
        if (cls == null) return null
        if (type is ParameterizedType) {
            val viewHoldType = findViewHoldType(type.actualTypeArguments)
            if (viewHoldType != null) {
                return viewHoldType
            }
        }
        return findViewHoldType(cls.genericSuperclass, cls.superclass)
    }

    private fun findViewHoldType(actualTypeArguments: Array<Type>?): Type? {
        if (actualTypeArguments == null) return null
        return actualTypeArguments.find {
            if (it is Class<*>) {
                return@find Holder::class.java.isAssignableFrom(it)
            }
            return@find false
        }
    }

}

class ChildA() : Base<Holder1>()

class ChildB() : Base<Holder2>()

abstract class ChildBase<A, B : Holder>() : Base<B>()

open class ChildC() : ChildBase<Int, Holder3>()
class ChildD() : ChildC()


