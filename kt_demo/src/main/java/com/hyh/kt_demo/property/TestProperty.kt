package com.hyh.kt_demo.property

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * TODO
 *
 * @author eriche 2022/10/10
 */
open class TestProperty<in T, V> constructor(
    initialValue: V,
) : ReadWriteProperty<T, V> {

    private var value: V = initialValue

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return value
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        before()
        this.value = value
        after()
    }


    protected open fun before() {

    }

    protected open fun after() {

    }
}


class TestPropertyTest {

    var ignore = false

    var flag: Boolean by (object : TestProperty<TestPropertyTest, Boolean>(false) {
        override fun before() {
            super.before()
        }

        override fun after() {
            super.after()
            println("$ignore")
        }
    })

}


fun main() {

    val testPropertyTest = TestPropertyTest()

    testPropertyTest.ignore = true
    testPropertyTest.flag = true
    testPropertyTest.flag = false
    testPropertyTest.ignore = false


}