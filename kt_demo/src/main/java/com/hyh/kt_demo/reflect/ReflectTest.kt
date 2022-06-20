package com.hyh.kt_demo.reflect

import com.hyh.kt_demo.ElementWeightProvider
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.system.measureTimeMillis

/**
 * TODO: Add Description
 *
 * @author eriche 2022/6/19
 */


class TestData() {

    var data1: String? = null

    val data2: String = "1"

    var data3: String = "2"


}


class XXXXX() {



    val data2: String = "1"

    var data3: String = "2"

    var data1: String? = null

}


fun main() {
    //TestData::class.declaredMemberProperties


    val testData = TestData()
    val testData2 = XXXXX()


    val time1 = measureTimeMillis {
        repeat(1) {
            val declaredMemberProperties = ElementWeightProvider::class.declaredMemberProperties
            /*declaredMemberProperties.forEach {
                it.isAccessible = true
                //println("${it.name} - ${it.get(testData)}")
                //it.get(testData)
            }*/
        }
    }


    val time2 = measureTimeMillis {
        repeat(1) {
            val declaredMemberProperties = testData2.javaClass.kotlin.declaredMemberProperties
            declaredMemberProperties.forEach {
                it.isAccessible = true
                //println("${it.name} - ${it.get(testData)}")
                it.get(testData2)
            }
        }
    }

    println("kotlin $time2")


    val time3 = measureTimeMillis {
        repeat(1) {
            val declaredFields = testData.javaClass.declaredFields
            declaredFields.forEach {
                it.isAccessible = true
                //println("${it.name} - ${it.get(testData)}")
                it.get(testData)
            }
        }
    }

    println("java $time3")

}