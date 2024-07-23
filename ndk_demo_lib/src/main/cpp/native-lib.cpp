#include <jni.h>
#include <string>
#include "..\..\..\..\ndk_cpp_lib\Circle.h"

extern "C" JNIEXPORT jstring
JNICALL
stringFromJNI(JNIEnv *env, jobject thiz)
{
	std::string hello =Test::getString();
	return env->NewStringUTF(hello.c_str());
}

static const JNINativeMethod nativeMethods[] = {
		// Java中的函数名, 函数签名信息, native的函数指针
		{"stringFromJNI", "()Ljava/lang/String;", (void *) (stringFromJNI)},
};


jint RegisterNatives(JNIEnv *env)
{
	jclass clazz = env->FindClass("com/example/ndk_demo_lib/TestJNI");
	if (clazz == NULL)
	{
		return JNI_ERR;
	}
	return env->RegisterNatives(
			clazz,
			nativeMethods,
			sizeof(nativeMethods) / sizeof(nativeMethods[0])
	);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env = NULL;
	if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK)
	{
		return JNI_ERR;
	}
	if (RegisterNatives(env) == JNI_OK)
	{
		return JNI_VERSION_1_6;
	}
	return JNI_ERR;
}