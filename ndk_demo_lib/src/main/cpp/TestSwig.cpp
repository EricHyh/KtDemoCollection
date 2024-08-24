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

void TestSwig::testCallback(const TestCallback& callback) const {
    callback(this->Area());
}

// 实现适配器函数
void testCallbackAdapter(TestSwig* self, TestCallbackWrapper* wrapper) {
    if (self && wrapper) {
        self->testCallback([wrapper](const double& value) {
            wrapper->call(value);
        });
    }
}
