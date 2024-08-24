#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>

using namespace std;

namespace const_test
{

    class IntValueWrapper
    {

    public:
        int value = 10;
        IntValueWrapper(int value) : value(value)
        {
            cout << "IntValueWrapper: " << this << endl;
        }
        ~IntValueWrapper()
        {
            cout << "~IntValueWrapper: " << this << endl;
        }
    };

    class ConstTest
    {

    public:
        IntValueWrapper *value;

        int value2;

        shared_ptr<IntValueWrapper> sp_value;

        ConstTest() : value(new IntValueWrapper(6)), sp_value(make_shared<IntValueWrapper>(6))
        {
            cout << "ConstTest: " << this << endl;
        };

        ConstTest(int value) : value(new IntValueWrapper(value)), sp_value(make_shared<IntValueWrapper>(value))
        {
            cout << "ConstTest(value): " << this << endl;
        };
        ~ConstTest()
        {
            cout << "~ConstTest: " << this << endl;
            delete value;
        };

        ConstTest(const ConstTest &other) noexcept
        {
            cout << "ConstTest copy: " << this << endl;
            this->value = new IntValueWrapper(*other.value); // 深拷贝
            this->sp_value = other.sp_value;                 // 浅拷贝
        }

        ConstTest(ConstTest &&other) noexcept : value(std::move(other.value)), sp_value(other.sp_value)
        {
            cout << "ConstTest move: " << this << endl;
            other.value = nullptr; // 防止 other 析构时删除 value
        }

        ConstTest &operator=(const ConstTest &other)
        {
            cout << "ConstTest operator=: " << this << endl;
            if (this != &other)
            {
                IntValueWrapper *newValue = new IntValueWrapper(*other.value); // 先创建新的
                delete value;                                                  // 再删除旧的
                value = newValue;                                              // 最后赋值

                sp_value = other.sp_value;
            }
            return *this;
        }

        // 移动赋值运算符
        ConstTest &operator=(ConstTest &&other) noexcept
        {
            cout << "ConstTest move operator=: " << this << endl;
            if (this != &other)
            {
                value = std::move(other.value);
                other.value = nullptr; // 防止 other 析构时删除 value

                sp_value = other.sp_value;
            }
            return *this;
        }

        int getValue() const
        {
            value->value = 100;
            return value->value;
        }
    };

    int getNum()
    {
        const int a = 10;
        return a;
    }

    ConstTest getConstTest()
    {
        const ConstTest ct;
        return ct;
    }

    void test1()
    {
        // int a = getNum();
        // cout << a << endl;
        const ConstTest ct1 = getConstTest();

        cout << ct1.value->value << endl;

        ConstTest ct2 = ct1;
        *ct2.value = 20;
        *ct2.sp_value = 30;
        cout << ct1.value->value << endl;
        cout << ct1.sp_value->value << endl;
    }

    void test2()
    {
        int num1 = 10;
        int num2 = 30;
        const int *numPtr1 = &num1;
        int const *numPtr2 = &num2;
        cout << *numPtr1 << endl;
        num1 = 20;
        cout << *numPtr1 << endl;

        numPtr1 = numPtr2;
        cout << *numPtr1 << endl;
    }

    void test3()
    {
        int num1 = 10;
        int num2 = 30;
        int *const ptr1 = &num1;
        int *const ptr2 = &num1;
        *ptr1 = 20;
        cout << *ptr1 << endl;
    }

    void test4()
    {
        int num1 = 10;
        int num2 = 30;

        const int *const ptr1 = &num1;
        const int *const ptr2 = &num2;

        num1 = 40;

        cout << *ptr1 << endl;
    }

    void test() {
        ConstTest ct;
        cout << ct.getValue() << endl;
    }

}