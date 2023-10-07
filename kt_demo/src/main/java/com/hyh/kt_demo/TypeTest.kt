package com.hyh.kt_demo

import java.lang.Exception
import java.lang.String
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.math.max

/**
 * TODO: Add Description
 *
 * @author eriche 2021/8/18
 */

fun main() {

    //    c  b  c  d  e  f
    // a  0  0  0  0  0  0
    // e  0  0  0  0  1  1
    // f  0  0  0  0  1  2
    // b  0  1  1  1  1  2
    // c  1  1  2  2  2  2
    // e  1  1  1  1  3  3

    //    c  b  c  d  e  f
    // a  0  0  0  0  0  0
    // e  0  0  0  0  1  0
    // f  0  0  0  0  0  1
    // b  0  1  0  0  0  0
    // c  1  0  1  0  0  0
    // e  0  0  0  0  1  0

    //bce

    //7 6 2 4 5 1 3
    val max = 0
    // 6 - 7 = -1
    // 1 - 7 = -6
    // 1 - 6 = -5
    // 2 - 7 = -5
    // 2 - 6 = -4
    // 2 - 1 = 1
    // 1

    // 7 6 2 4 5 -1 1 3
    // 7 6 2 4 5 -5 1 3
    // f(0 ,0) = arr[0]
    // f(0, 1) = max(arr[0], arr[0] + arr[1])
    // f(1, 1) = arr[1]
    // f(a, b) = max(f(a - 1, b), f(a - 1, b) + arr[a - 1])
    // f(a, b) = max(f(a, b - 1), f(a, b - 1) + arr[b])
    //


    // f(b) = max(f(b - 1), f(b - 1) + aar[b])


    /*val xx =
        "283445307142697904:9|283445320027599792:11|283726802294245040:11|281756462277401264:3|281756457682433968:2|281756461977401264:8|281756457982433968:7|281756462077401264:1|281756457782433968:3|281756455982434268:12|281756455982446768:11|281756457882433968:6|281756462177401264:0|281756460277401264:1|281756468867335856:5|281756473162303152:5|"


    val xxx: java.lang.String = String("283445307142697904:9|283445320027599792:11")

    val split = xxx.split("|")
    val split1 = xxx.split("\\|")

    ChildD().test()
    ChildA().test()
    ChildB().test()
    ChildC().test()*/


    A1().test()
    A2().test()
    A3().test()
    A4().test()
    A5().test()
    A6().test()

    println()

}


fun maxNum(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    // f(b) = max(f(b - 1), f(b - 1) + aar[b])
    var max = 0
    val maxArray = IntArray(nums.size)
    maxArray[0] = max(max, nums[0])
    for (index in 1 until nums.size) {
        maxArray[index] = max(maxArray[index - 1], maxArray[index - 1] + nums[index])
        max = max(max, maxArray[index])
    }
    return max
}


fun maxProfit(prices: IntArray): Int {
    if (prices.size <= 1) return 0
    var max = 0
    var left: Int = prices[0]
    for (index in 1 until prices.size) {
        val price = prices[index]
        val diff = price - left
        if (diff < 0) {
            left = price
        } else {
            max = max(max, diff)
        }
    }
    return max
}

abstract class A {

    fun test() {
        val findMethodDeclaredClass = findMethodDeclaredClass("methodA")
        println(findMethodDeclaredClass)
    }

    private fun findMethodDeclaredClass(methodName: kotlin.String): Class<*>? {
        var cls: Class<*>? = this::class.java
        while (cls != null && cls != A::class.java) {
            val declaredMethod =
                try {
                    cls.getDeclaredMethod(methodName)
                } catch (e: Exception) {
                    null
                }
            if (declaredMethod != null) return cls
            cls = cls.superclass
        }
        return null
    }

    open fun methodA() {
    }
}


class A1 : A()
open class A2 : A() {
    override fun methodA() {
        super.methodA()
    }
}

class A3 : A2() {
    override fun methodA() {
        super.methodA()
    }
}

class A4 : A2() {
}


open class A5 : A()
class A6 : A5() {
    override fun methodA() {
        super.methodA()
    }
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


