%module SwigDemo

// 启用 director 功能，允许 Java 继承和重写 C++ 类
%feature("director") TestCallbackWrapper;

%{
#include "TestSwig.h"
%}


// 包含原始的 TestSwig 头文件
%include "TestSwig.h"