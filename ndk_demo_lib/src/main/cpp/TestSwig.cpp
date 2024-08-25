//
// Created by eriche on 2024/8/24.
//

#include "TestSwig.h"

TestSwig::TestSwig() : r(100) {

}

TestSwig::TestSwig(double R) : r(R) {

}

double TestSwig::Area() const {
    return this->r * this->r;
}

void TestSwig::test1(const TestSwigData &data) const {

}

void TestSwig::test2(const TestCallback& callback) const {
    callback(TestSwigData());
}


// 实现适配器函数
void testCallbackAdapter(TestSwig* self, TestCallbackWrapper* wrapper) {
    if (self && wrapper) {
        self->test2([wrapper](const TestSwigData& value) {
            wrapper->call(value);
        });
    }
}
