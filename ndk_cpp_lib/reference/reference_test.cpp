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

    void test()
    {
        
    }

}