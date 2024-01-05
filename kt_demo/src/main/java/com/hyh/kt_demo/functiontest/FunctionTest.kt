package com.hyh.kt_demo.functiontest

import com.hyh.kt_demo.A

/**
 * TODO: Add Description
 *
 * @author eriche 2023/11/21
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class MethodFormulaDependent(val value: Int)


object FunctionTest {


    fun test1(
        @MethodFormulaDependent(1)
        a: Int,
        @MethodFormulaDependent(2)
        b: Int
    ): Int {
        return a + b
    }

}


fun main() {
    val kFunction3 = FunctionTest::test1

    val declaredMethods = kFunction3::class.java.declaredMethods
    val method = declaredMethods[0]
    val parameterTypes = method.parameterTypes

    val realMethod = FunctionTest::class.java.getDeclaredMethod(kFunction3.name, *parameterTypes)
    val parameterAnnotations = realMethod.parameterAnnotations

    println("")

    val gsonComplexSettingInfoStore = object : GsonComplexSettingInfoStore<Map<Int, List<Int>>>() {
    }


    val toConfigStr = gsonComplexSettingInfoStore.toConfigStr(
        mapOf(
            0 to listOf(1, 2, 3),
            1 to listOf(11, 12, 13),
            2 to listOf(21, 22, 23),
        )
    )

    val parseConfigStr = gsonComplexSettingInfoStore.parseConfigStr(toConfigStr)




    val constructor = TestData::class.java.getDeclaredConstructor(
        true.javaClass,
        true.javaClass,
        Boolean::class.javaObjectType,
        XX::class.java,
        XX::class.java,
    )

    //val newInstance = constructor.newInstance(true, true, null, null, null)

    val java = Boolean::class.java

    val booleanClass = JavaClassTest.getBooleanClass()


    println()

}


data class TestData(
    val a1: Boolean,
    val a2: Boolean,
    val a3: Boolean?,
    val a: XX,
    val b: XX,
)


enum class XX{

    AA,
    BB

}

