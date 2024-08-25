//
// Created by eriche on 2024/8/25.
//

#pragma once

#include <string>
#include <vector>



class TestSwigData2 {

public:
    int value1;

    unsigned int value2;

    long value3;

    unsigned long value4;

    float value5;

    double value6;

    std::string value7;

};

class TestSwigData {

public:
    int value1;

    unsigned int value2;

    long value3;

    unsigned long value4;

    float value5;

    double value6;

    std::string value7;

    std::vector<int> values1;
    std::vector<std::string> values2;
    std::vector<TestSwigData2> values3;

};