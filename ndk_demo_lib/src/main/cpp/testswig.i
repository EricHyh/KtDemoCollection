%module SwigDemo

%{
#include <functional>
#include "TestSwigData.h"
#include "TestSwig.h"
%}

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

// 忽略原始的 testCallback 方法
%ignore TestSwig::test2;

// 启用 std::string 到 java.lang.String 的转换
%include "std_string.i"

// 如果您的代码中使用了 std::vector<std::string>，还需要添加：
%include "std_vector.i"
%template(StringVector) std::vector<std::string>;
%template(IntVector) std::vector<int>;
%template(TestSwigData2Vector) std::vector<TestSwigData2>;

// 包含原始的 TestSwig 头文件
%include "TestSwigData.h"
%include "TestSwig.h"