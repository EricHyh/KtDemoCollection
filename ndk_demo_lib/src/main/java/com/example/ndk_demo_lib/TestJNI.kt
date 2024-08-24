package com.example.ndk_demo_lib


/**
 * TODO: Add Description
 *
 * @author eriche 2024/7/19
 */
class TestJNI<T> {
    companion object {
        private const val TAG = "TestJNI"

        init {
            System.loadLibrary("native-lib");
        }
    }

    external fun stringFromJNI(): T

    external fun testCall1(callback: TestCallbackJni<TestData1>)

    external fun testCall2(callback: TestCallbackJni<TestData2>)
}


class TestData1 constructor(
    val value1: Int,
    val value2: String,
)

class TestData2 constructor(
    val value1: Int,
    val value2: String,
)


fun interface TestCallbackJni<T> {

    fun onTest(data: T)

}