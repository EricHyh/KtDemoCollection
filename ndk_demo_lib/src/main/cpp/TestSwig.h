//
// Created by eriche on 2024/8/24.
//

#pragma once

#include <string>
#include <functional>

//class TestCallback {
//public:
//    static TestCallback* newCallback();
//    TestCallback() {} // 添加空构造函数
//    virtual void onTest(const double & msg) = 0;
//    virtual ~TestCallback() {}
//};

using TestCallback = std::function<void(const double &)>;



class TestSwig {

private:
    double r{}; // 半径
public:
    TestSwig();         // 构造函数
    TestSwig(double R); // 构造函数
    double Area() const;    // 求面积函数

    void testCallback(const TestCallback &callback) const;

};

// 添加这个声明
class TestCallbackWrapper {
public:
    virtual ~TestCallbackWrapper() {}
    virtual void call(const double& value) = 0;
};
void testCallbackAdapter(TestSwig* self, TestCallbackWrapper* wrapper);