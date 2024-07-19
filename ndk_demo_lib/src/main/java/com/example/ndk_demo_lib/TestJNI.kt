package com.example.ndk_demo_lib


/**
 * TODO: Add Description
 *
 * @author eriche 2024/7/19
 */
class TestJNI {
    companion object {
        private const val TAG = "TestJNI"
        init {
            System.loadLibrary("native-lib");
        }
    }

    external fun stringFromJNI(): String

}