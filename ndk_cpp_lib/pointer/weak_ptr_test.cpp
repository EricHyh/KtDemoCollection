#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>

using namespace std;

class WeakPtrTest
{
private:
    /* data */
public:
    WeakPtrTest()
    {
        cout << "WeakPtrTest: " << this << endl;
    };

    WeakPtrTest(int value) : value(value)
    {
        cout << "WeakPtrTest: " << this << endl;
    };

    ~WeakPtrTest()
    {
        cout << "~WeakPtrTest: " << this << endl;
    };

    int value;
};

namespace weak_ptr_test
{

    class Parent; // Parent类的前置声明

    class Child
    {
    public:
        Child() { cout << "Child" << endl; }
        ~Child() { cout << "~Child" << endl; }

        weak_ptr<Parent> parent;
    };

    class Parent
    {
    public:
        Parent() { cout << "Parent" << endl; }
        ~Parent()
        {
            cout << "~Parent" << endl;
        }

        shared_ptr<Child> child;
    };

    void test1()
    {
        shared_ptr<WeakPtrTest> sp1(new WeakPtrTest());
        weak_ptr<WeakPtrTest> wp2(sp1);
        cout << sp1.use_count() << endl;
        cout << wp2.use_count() << endl;
        weak_ptr<WeakPtrTest> wp3 = wp2;
        cout << sp1.use_count() << endl;
        cout << wp2.use_count() << endl;
        cout << wp3.use_count() << endl;

        shared_ptr<WeakPtrTest> sp2 = sp1;
        cout << sp1.use_count() << endl;
        cout << wp2.use_count() << endl;
        cout << wp3.use_count() << endl;
    }

    void test2()
    {
        shared_ptr<WeakPtrTest> sp1(new WeakPtrTest(10));
        weak_ptr<WeakPtrTest> wp1(sp1);
        cout << sp1->value << endl;
        if (!wp1.expired())
        {
            wp1.lock()->value = 11;
            cout << wp1.lock()->value << endl;
        }
        wp1.reset();
        if (!wp1.expired())
        {
            wp1.lock()->value = 11;
            cout << wp1.lock()->value << endl;
        }
        else
        {
            cout << "wp1 expired" << endl;
            cout << sp1->value << endl;
        }
    }

    void test()
    {
        shared_ptr<Child> child(new Child());
        shared_ptr<Parent> parent(new Parent());

        child->parent = parent;
        parent->child = child;
    }
}