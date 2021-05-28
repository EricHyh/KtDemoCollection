package com.hyh.kt_demo.compare

import kotlin.math.abs


object SortState {
    /**
     * 原生排序.
     */
    const val STATE_NATIVE = 0

    /**
     * 升序.
     */
    const val STATE_UP = 1

    /**
     * 降序.
     */
    const val STATE_DOWN = 2

}


/**
 * 支持比较的值
 */
interface ComparableValue<T> {

    val value: T?

    /**
     * 比较两个值的大小，考虑排序方式
     *
     * @param other 要比较的目标
     * @param sortState 排序方式
     * @return
     */
    fun compareTo(other: ComparableValue<T>?, sortState: Int): Int {
        val compare = compareTo(other)
        return compare * (if (sortState == SortState.STATE_DOWN) -1 else 1)
    }

    /**
     * 比较两个值的大小
     *
     * @param other 要比较的目标
     * @return
     */
    fun compareTo(other: ComparableValue<T>?): Int

}

/**
 * 字符串值比较
 */
class StringCompareValue(override val value: String?) : ComparableValue<String?> {

    override fun compareTo(other: ComparableValue<String?>?): Int {
        if (other !is StringCompareValue) {
            return 1
        }
        return run {
            val thisValue = this.value
            val otherValue = other.value
            when {
                thisValue == null -> -1
                otherValue == null -> 1
                else -> thisValue.compareTo(otherValue)
            }
        }
    }
}

/**
 * Number类型比较基类
 */
abstract class NumberCompareValue<T : Number>(override val value: T?) : ComparableValue<T> {


    override fun compareTo(other: ComparableValue<T>?): Int {
        if (other !is NumberCompareValue<T>) {
            return 1
        }
        val thisValue = value
        val otherValue = other.value
        return when {
            thisValue == null -> -1
            otherValue == null -> 1
            else -> compare(thisValue, otherValue)
        }
    }

    fun compareAbsTo(other: ComparableValue<T>?): Int {
        if (other !is NumberCompareValue<T>) {
            return 1
        }
        val thisValue = value
        val otherValue = other.value
        return when {
            thisValue == null -> -1
            otherValue == null -> 1
            else -> compareAbs(thisValue, otherValue)
        }
    }

    abstract fun compare(thisValue: T, otherValue: T): Int

    protected abstract fun compareAbs(thisValue: T, otherValue: T): Int

}

/**
 * 基础数据类型的Number比较
 */
open class PrimitiveNumberCompareValue<T : Number>(override val value: T?) : NumberCompareValue<T>(value) {

    override fun compare(thisValue: T, otherValue: T): Int {
        return when (thisValue) {
            is Byte -> {
                thisValue.compareTo(otherValue as Byte)
            }
            is Short -> {
                thisValue.compareTo(otherValue as Short)
            }
            is Int -> {
                thisValue.compareTo(otherValue as Int)
            }
            is Long -> {
                thisValue.compareTo(otherValue as Long)
            }
            is Float -> {
                thisValue.compareTo(otherValue as Float)
            }
            is Double -> {
                thisValue.compareTo(otherValue as Double)
            }
            else -> 1
        }
    }

    override fun compareAbs(thisValue: T, otherValue: T): Int {
        return when (thisValue) {
            is Byte -> {
                abs(thisValue.toInt()).compareTo(abs((otherValue as Byte).toInt()))
            }
            is Short -> {
                abs(thisValue.toInt()).compareTo(abs((otherValue as Short).toInt()))
            }
            is Int -> {
                abs(thisValue).compareTo(abs(otherValue as Int))
            }
            is Long -> {
                abs(thisValue).compareTo(abs(otherValue as Long))
            }
            is Float -> {
                abs(thisValue).compareTo(abs(otherValue as Float))
            }
            is Double -> {
                abs(thisValue).compareTo(abs(otherValue as Double))
            }
            else -> 1
        }
    }
}


typealias ByteCompareValue = PrimitiveNumberCompareValue<Byte>
typealias ShortCompareValue = PrimitiveNumberCompareValue<Short>
typealias IntCompareValue = PrimitiveNumberCompareValue<Int>
typealias LongCompareValue = PrimitiveNumberCompareValue<Long>
typealias FloatCompareValue = PrimitiveNumberCompareValue<Float>
typealias DoubleCompareValue = PrimitiveNumberCompareValue<Double>


/**
 * 字符串Double值比较
 */
class DoubleStringCompareValue(value: String?) : PrimitiveNumberCompareValue<Double>(parseDouble(value, 0.0))


/**
 * 数值绝对值比较
 */
class AbsoluteNumberCompareValue(override val value: NumberCompareValue<*>?) : ComparableValue<NumberCompareValue<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun compareTo(other: ComparableValue<NumberCompareValue<*>>?): Int {
        if (other !is AbsoluteNumberCompareValue) {
            return 1
        }
        val thisValue = this.value as? NumberCompareValue<Number>
        val otherValue = other.value as? NumberCompareValue<Number>
        return when {
            thisValue == null -> -1
            otherValue == null -> 1
            else -> thisValue.compareAbsTo(otherValue)
        }
    }
}


/**
 * 比较结果反转
 */
class ReverseComparableValue(override val value: ComparableValue<*>?) : ComparableValue<ComparableValue<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun compareTo(other: ComparableValue<ComparableValue<*>>?): Int {
        if (other !is ReverseComparableValue) {
            return -1
        }
        val thisValue = this.value as? ComparableValue<Any>
        val otherValue = other.value as? ComparableValue<Any>
        return when {
            thisValue == null -> 1
            otherValue == null -> -1
            else -> -thisValue.compareTo(otherValue)
        }
    }
}


fun ComparableValue<*>.toReverseValue(): ComparableValue<ComparableValue<*>> {
    return ReverseComparableValue(this)
}

fun NumberCompareValue<out Number>.toAbsoluteValue(): ComparableValue<NumberCompareValue<out Number>> {
    return AbsoluteNumberCompareValue(this)
}


fun main() {

    val doubleStringCompareValue1 = DoubleStringCompareValue("3")  as ComparableValue<Any>
    val doubleStringCompareValue2 = DoubleCompareValue(2.0)  as ComparableValue<Any>

    /*val compareTo1 = doubleStringCompareValue1.toAbsoluteValue().compareTo(doubleStringCompareValue2.toAbsoluteValue())

    val compareTo2 = doubleStringCompareValue1.toReverseValue().toReverseValue().compareTo(doubleStringCompareValue2.toReverseValue().toReverseValue())*/


    val compareTo = doubleStringCompareValue1.compareTo(doubleStringCompareValue2)

    println(compareTo)
}


fun parseDouble(value: String?, defaultVal: Double): Double {
    var result = defaultVal
    try {
        result = value?.toDouble() ?: defaultVal
    } catch (e: Exception) {
    }
    return result
}