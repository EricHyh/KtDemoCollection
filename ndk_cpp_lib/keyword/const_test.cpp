#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>
#include <vector>

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

        void good(int *other) const
        {
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

    void test5()
    {
        ConstTest ct;
        cout << ct.getValue() << endl;
    }

    void const_test1(
        const int &num1, // 常量引用
        int const &num2, // 常量引用

        int const *num4,       // 常量指针
        int *const num5,       // 指针常量
        const int *const num6, // 指向常量的指针常量

        int const *&num7,       // 常量指针的引用
        const int *const &num10 // 指向常量的指针常量的引用
    )
    {
        // num1 = num1 + 10;
        // num2 = num2 + 10;
        // num3 = num3 + 10;

        // *num4 = *num4 + 10;
        // num4 = new int(*num4 + 10);

        // *num5 = *num5 + 10;
        // num5 = new int(*num5 + 10);

        // *num6 = *num6 + 10;
        // num6 = new int(*num6 + 10);

        // *num7 = *num7 + 10;
        // num7 = new int(*num7 + 10);

        // *num8 = *num8 + 10;
        // num8 = new int(*num8 + 10);

        // *num9 = *num9 + 10;
        // num9 = new int(*num9 + 10);

        // *num9 = *num9 + 10;
        // num10 = new int(*num10 + 10);
    }

    void test6()
    {
        int num1 = 1;
        int num2 = 2;

        int num4 = 4;
        int num5 = 5;
        int num6 = 6;

        int num7 = 7;
        int num10 = 10;

        int *num4_ptr = &num4;
        int *num5_ptr = &num5;
        int *const num6_ptr = &num6;

        int const *num7_ptr = &num7;
        int *num10_ptr = &num10;

        const_test1(num1, num2, num4_ptr, num5_ptr, num6_ptr, num7_ptr, num10_ptr);
    }

    const int getIntNum1()
    {
        return 1;
    }

    const int &getIntNum2()
    {
        return 1;
    }

    const int *getIntNum3()
    {
        return new int(1);
    }

    int *const getIntNum4()
    {
        return new int(1);
    }

    const int *const getIntNum5()
    {
        return new int(1);
    }

    void test7()
    {
        int num1 = getIntNum1();

        int num2 = getIntNum2();

        const int *num33 = getIntNum3();

        int *num4 = getIntNum4();
        const int *num44 = getIntNum4();

        const int *num5 = getIntNum5();
        const int *const num55 = getIntNum5();
    }

    void test()
    {
        vector<int> vec = {1, 2, 3};

        for (vector<int>::iterator it = vec.begin(); it != vec.end(); ++it)
        {
            *it = *it + 10;
            cout << *it << endl;
        }

        for (vector<int>::const_iterator it = vec.begin(); it != vec.end(); ++it)
        {
            cout << *it << endl;
        }

        for (int &num : vec)
        {
            num = num + 10;
            cout << num << " " << endl;
        }

        for (const int &num : vec)
        {
            cout << num << " " << endl;
        }

    }
}