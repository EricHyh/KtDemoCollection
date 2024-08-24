package com.example.ndk_demo_lib;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.ByVal;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Namespace;
import org.bytedeco.javacpp.annotation.Platform;


@Platform(include="TestJavaCpp.h", library="native-lib")
@Namespace("std")
public class TestJavaCpp extends Pointer {
    static { Loader.load(); }

    @Name("TestJavaCpp")
    public static class TestJavaCppClass extends Pointer {
        static { Loader.load(); }
        public TestJavaCppClass() { allocate(); }
        private native void allocate();

        // 映射 C++ 的 test 方法
        public native void test(@ByVal TestJavaCppCallback callback);
    }

    @Platform(include="<functional>")
    public static class TestJavaCppCallback extends FunctionPointer {
        static { Loader.load(); }
        protected TestJavaCppCallback() { allocate(); }
        private native void allocate();

        // 这个方法对应 C++ 中的 bool(const int&)
        public native boolean call(int value);
    }
}