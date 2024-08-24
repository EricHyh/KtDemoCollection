#include <jni.h>
#include <string>
#include "..\..\..\..\ndk_cpp_lib\Circle.h"

extern "C" JNIEXPORT jobject
JNICALL
stringFromJNI(JNIEnv *env, jobject thiz) {
    //std::string hello = Test::getString();
    //return env->NewStringUTF(hello.c_str());
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
    jobject result = env->NewObject(integerClass, integerConstructor, 100);
    return result;
}

static const JNINativeMethod nativeMethods[] = {
        // Java中的函数名, 函数签名信息, native的函数指针
        //{"stringFromJNI", "()Ljava/lang/String;", (void *) (stringFromJNI)},
        {"stringFromJNI", "()Ljava/lang/Object;", (void *) (stringFromJNI)},
};


jint RegisterNatives(JNIEnv *env) {
    jclass clazz = env->FindClass("com/example/ndk_demo_lib/TestJNI");
    if (clazz == NULL) {
        return JNI_ERR;
    }
    return env->RegisterNatives(
            clazz,
            nativeMethods,
            sizeof(nativeMethods) / sizeof(nativeMethods[0])
    );
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    if (RegisterNatives(env) == JNI_OK) {
        return JNI_VERSION_1_6;
    }
    return JNI_ERR;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ndk_1demo_1lib_TestJNI_testCall1(JNIEnv *env, jobject thiz, jobject callback) {
    //com.example.ndk_demo_lib.TestData1
    jclass testData1Class = env->FindClass("com/example/ndk_demo_lib/TestData1");
    jmethodID testData1Constructor = env->GetMethodID(testData1Class, "<init>", "(ILjava/lang/String;)V");

    //
    jobject testData1 = env->NewObject(testData1Class, testData1Constructor, 10, env->NewStringUTF("100"));

    //com.example.ndk_demo_lib.TestCallback
    jclass testCallbackClass = env->FindClass("com/example/ndk_demo_lib/TestCallbackJni");
    jmethodID onTest = env->GetMethodID(testCallbackClass, "onTest", "(Ljava/lang/Object;)V");

    env->CallVoidMethod(callback, onTest, testData1);

    env->DeleteLocalRef(testData1);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ndk_1demo_1lib_TestJNI_testCall2(JNIEnv *env, jobject thiz, jobject callback) {
    //com.example.ndk_demo_lib.TestData2
    jclass testData2Class = env->FindClass("com/example/ndk_demo_lib/TestData2");
    jmethodID testData1Constructor = env->GetMethodID(testData2Class, "<init>", "(ILjava/lang/String;)V");

    //
    jobject testData2 = env->NewObject(testData2Class, testData1Constructor, 20, env->NewStringUTF("200"));

    //com.example.ndk_demo_lib.TestCallback
    jclass testCallbackClass = env->FindClass("com/example/ndk_demo_lib/TestCallbackJni");
    jmethodID onTest = env->GetMethodID(testCallbackClass, "onTest", "(Ljava/lang/Object;)V");

    env->CallVoidMethod(callback, onTest, testData2);

    env->DeleteLocalRef(testData2);
}