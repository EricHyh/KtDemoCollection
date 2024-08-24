%module SwigDemo

%{
#include <functional>
#include "TestSwig.h"
%}

// 启用 director 功能，允许 Java 继承和重写 C++ 类
%feature("director") TestCallbackWrapper;

// 修改 TestSwig 类的 testCallback 方法
%extend TestSwig {
        void testCallback(TestCallbackWrapper* callback) {
            if (callback) {
                self->testCallback([callback](const double& value) {
                    callback->call(value);
                });
            }
        }
}

// 忽略原始的 testCallback 方法
%ignore TestSwig::testCallback;

// 包含原始的 TestSwig 头文件
%include "TestSwig.h"