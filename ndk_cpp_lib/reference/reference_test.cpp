#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>

using namespace std;

class ReferenceTest
{
private:
    /* data */
public:
    ReferenceTest()
    {
        cout << "ReferenceTest: " << this << endl;
    };

    ReferenceTest(int value) : value(value)
    {
        cout << "ReferenceTest: " << this << endl;
    };

    ~ReferenceTest()
    {
        cout << "~ReferenceTest: " << this << endl;
    };

    // 复制构造函数
    ReferenceTest(const ReferenceTest &other) : value(other.value)
    {
        std::cout << "Copy constructor called: " << this << std::endl;
    }

    int value;
};

namespace reference_test
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
        int a = 10;
        int b = a;
        int &c = a;
        int &d = c;
        c = b;

        c = 20;

        cout << a << endl;
        cout << b << endl;
        cout << c << endl;
        cout << d << endl;
    }

    int &getInt()
    {
        static int a = 10;
        return a;
    }

    ReferenceTest &getReferenceTest()
    {
        static ReferenceTest rt(10);
        return rt;
    }

    void test2()
    {
        int &a = getInt();
        cout << a << endl;
    }

    void test3()
    {
        ReferenceTest rt1 = getReferenceTest();
        ReferenceTest &rt2 = getReferenceTest();

        cout << rt1.value << endl;
        cout << rt2.value << endl;
    }

    struct TestPtrRsult
    {
        int *result1;
        int &result2;
        int *&result3;
    };

    TestPtrRsult testPtr(int *other1, int &other2, int *&other3)
    {
        int *num1_ptr = new int(10);
        other2 = 20;
        int *num3_ptr = new int(30);

        other1 = num1_ptr;
        other3 = num3_ptr;

        return {num1_ptr, other2, other3};
    }

    void test()
    {
        int num1 = 1;
        int num2 = 2;
        int num3 = 3;

        int *num1_ptr = &num1;
        int &num2_ref = num2;
        int *num3_ptr = &num3;

        TestPtrRsult test_ptr = testPtr(num1_ptr, num2_ref, num3_ptr);

        cout << "*num1_ptr=" << *num1_ptr << endl;
        cout << "num2_ref=" << num2_ref << endl;
        cout << "*num3_ptr=" << *num3_ptr << endl;

        delete test_ptr.result1;
        delete test_ptr.result3;
    }

}