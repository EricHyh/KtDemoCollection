//
// Created by eriche on 2024/8/24.
//

#pragma once

#include "functional"

using TestJavaCppCallback = std::function<bool(const int &)>;


class TestJavaCpp {

public:
    void test(TestJavaCppCallback callback);

};
