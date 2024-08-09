#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>

using namespace std;

class UniquePtrTest
{
private:
    /* data */
public:
    UniquePtrTest()
    {
        cout << "UniquePtrTest: " << this << endl;
    };

    UniquePtrTest(int value) : value(value)
    {
        cout << "UniquePtrTest: " << this << endl;
    };

    ~UniquePtrTest()
    {
        cout << "~UniquePtrTest: " << this << endl;
    };

    int value;
};

namespace unique_ptr_test
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
        unique_ptr<UniquePtrTest> up1 = make_unique<UniquePtrTest>(10);
        UniquePtrTest *upt = up1.get();

        cout << up1->value << endl;

        unique_ptr<UniquePtrTest> up2 = move(up1);
        cout << up2->value << endl;

        shared_ptr<UniquePtrTest> sp1 = move(up2);
        sp1->value = 20;
        cout << upt->value << endl;

        unique_ptr<UniquePtrTest> up3(sp1.get());
        sp1.reset();
        up3->value = 30;
        cout << upt->value << endl;

        cout << "test end" << endl;
    }

    void test()
    {
        unique_ptr<UniquePtrTest> up1 = make_unique<UniquePtrTest>(10);
        unique_ptr<UniquePtrTest> up2 = make_unique<UniquePtrTest>(20);

        cout << up1.get() << endl;
        UniquePtrTest *upt = up1.release();
        cout << up1.get() << endl;

        up2.reset(upt);
        cout << up2.get() << endl;
        cout << up2->value << endl;
    }
}