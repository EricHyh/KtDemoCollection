//
// Created by eriche on 2024/8/31.
//

#pragma once

#include <memory>

class Callback1 {
public:
    void onTest(int a) {};

    ~Callback1() {};
};

class Callback2 {
public:
    virtual void onTest(int a) = 0;

    virtual ~Callback2() {};
};


class CallbackTest {

public:
    void addCallback(Callback1 callback1);

    void removeCallback(Callback1 callback1);

    void setCallback(Callback2 callback2);

    void setCallback2(std::shared_ptr<Callback2> callback2);

};


