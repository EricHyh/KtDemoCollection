%module SwigDemo

////%typemap(javapackage) Callback2 "com.example.xxx"
//%typemap(javapackage) SWIGTYPE "com.example.xx1"
//
//%pragma(java) moduleimports=%{
//import com.example.your.package.name.*;
//%}

//%feature("javacode") Callback2 %{
//// 你的自定义代码放在这里
//import java.util.*;
//%}

%{
#include <functional>
#include "TestSwigData.h"
#include "TestSwig.h"
#include <unordered_map>
#include <jni.h>
#include <memory>
// 全局变量来存储 Java 对象的全局引用
std::unordered_map<Callback1*, jobject> g_callbackRefs;
std::unordered_map<Callback2*, jobject> g_callback2Refs;
%}

%typemap(javabody) Callback2 %{

    static {
        System.loadLibrary("mylib");
    }
%}


%typemap(javacode) Callback2 %{
  // This is a test comment
  public void testMethod() {
    System.out.println("Test method");
  }
%}

%typemap(javaimports) Callback2 %{
// This is a test comment
package com.xxx.xxx;
%}



%include "std_shared_ptr.i"

// 启用 director 功能，允许 Java 继承和重写 C++ 类
%feature("director") TestCallbackWrapper;

// 修改 TestSwig 类的 testCallback 方法
%extend TestSwig {
        void test2(TestCallbackWrapper* callback) {
            if (callback) {
                self->test2([callback](const TestSwigData& value) {
                    callback->call(value);
                });
            }
        }
}

%typemap(in) Callback1 CallbackTest::addCallback {
    Callback1 *argp = *(Callback1 **)&jarg$argnum;
    if (!argp) {
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Attempt to dereference null Callback1");
    return $null;
    }
    $1 = *argp;
}

%typemap(argout) Callback1 CallbackTest::addCallback{
    Callback1 *argp = *(Callback1 **)&jarg$argnum;
    jobject globalRef = jenv->NewGlobalRef(jarg$argnum_);
    g_callbackRefs[argp] = globalRef;
}

%typemap(in) Callback1 (CallbackTest::removeCallback) {
    Callback1 *argp = *(Callback1 **)&jarg$argnum;
    if (!argp) {
        SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Attempt to dereference null Callback1");
        return $null;
    }
    $1 = *argp;

    auto it = g_callbackRefs.find(argp);
    if (it != g_callbackRefs.end()) {
        jenv->DeleteGlobalRef(it->second);
        g_callbackRefs.erase(it);
    }
}

%shared_ptr(Callback2)

%typemap(in) Callback2 (CallbackTest::setCallback) {
        Callback2 *argp = *(Callback2 **)&jarg$argnum;
        if (!argp) {
            SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Attempt to dereference null Callback2");
            return $null;
        }
        $1 = *argp;

        auto it = g_callback2Refs.find(argp);
        if (it != g_callback2Refs.end()) {
            jenv->DeleteGlobalRef(it->second);
            g_callbackRefs.erase(it);
        }
}


%typemap(in) std::shared_ptr<Callback2> (CallbackTest::setCallback2) {
        std::shared_ptr<Callback2> *argp = *(std::shared_ptr<Callback2> **)&jarg$argnum;
        if (argp) {
            // 创建全局引用
            jobject globalRef = jenv->NewGlobalRef(jarg$argnum_);

            // 创建新的 shared_ptr，使用自定义删除器
            $1 = std::shared_ptr<Callback2>(argp->get(), [globalRef](Callback2* ptr) {
                JNIEnv* env;
                if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) {
                    env->DeleteGlobalRef(globalRef);
                }
                delete ptr;
            });
        }
}


// 忽略原始的 testCallback 方法
%ignore TestSwig::test2;

// 启用 std::string 到 java.lang.String 的转换
%include "std_string.i"

// 如果您的代码中使用了 std::vector<std::string>，还需要添加：
%include "std_vector.i"
%template(StringVector) std::vector<std::string>;
%template(IntVector) std::vector<int>;
%template(TestSwigData2Vector) std::vector<TestSwigData2>;


//%shared_ptr(Callback1)

%feature("director") Callback1;
%feature("director") Callback2;



// 修改 Callback1 的构造函数
//%javamethodmodifiers Callback1::Callback1 "public";
//%rename(Callback1) Callback1::Callback1;
//%feature("javacode") Callback1::Callback1 %{
//{
//SwigDemoJNI.Callback1_director_connect(this, swigCPtr, true, false);
//}
//%}

// 包含原始的 TestSwig 头文件
%include "TestSwigData.h"
%include "TestSwig.h"
%include "test1/CallbackTest.h"