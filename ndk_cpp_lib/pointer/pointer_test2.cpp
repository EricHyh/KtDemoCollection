#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>

using namespace std;

class IntPointerTest
{
public:
    explicit IntPointerTest(int value) : value(new int(value))
    {
        cout << "IntPointerTest" << endl;
    };
    ~IntPointerTest()
    {
        delete value;
        cout << "~IntPointerTest" << endl;
    };
    int getValue()
    {
        return *value;
    }

    // 移动构造函数
    IntPointerTest(IntPointerTest &&other) noexcept : value(other.value)
    {
        other.value = nullptr;
        cout << "Move Constructor" << endl;
    }

    // 复制构造函数
    IntPointerTest(const IntPointerTest &other) : value(new int(*other.value))
    {
        std::cout << "Copy constructor called" << std::endl;
    }

    // 移动赋值运算
    IntPointerTest &operator=(IntPointerTest &&other) noexcept
    {
        if (this != &other)
        {
            delete value;
            value = other.value;
            other.value = nullptr;
        }
        cout << "Move Assignment" << endl;
        return *this;
    }

    // 复制赋值运算
    IntPointerTest &operator=(const IntPointerTest &other)
    {
        if (this != &other)
        {
            *value = *other.value;
        }
        std::cout << "Copy assignment operator called" << std::endl;
        return *this;
    }

private:
    int *value;
};

IntPointerTest getIntPointerTest(bool condition)
{
    IntPointerTest ipt1(5);
    IntPointerTest ipt2(10);
    cout << "getIntPointerTest ipt1: " << &ipt1 << endl;
    cout << "getIntPointerTest ipt2: " << &ipt2 << endl;
    if (condition)
    {
        return ipt1;
    }
    else
    {
        return ipt2;
    }
}

void badThing()
{
    throw 1; // 抛出一个异常
}

void test()
{
    IntPointerTest a(5);

    badThing();
}

// int main()
// {
//     try
//     {
//         test();
//     }
//     catch (int i)
//     {
//         cout << "error happened " << i << endl;
//     }

//     cout << "End of main()" << endl;

//     // IntPointerTest ipt = getIntPointerTest(true);

//     // cout << "main: " << &ipt << endl;

//     // cout << ipt.getValue() << endl;

//     // IntPointerTest ipt3 = ipt;

//     // cout << "ipt3: " << &ipt3 << endl;

//     // IntPointerTest a(5);
//     // cout << "main: " << &a << endl;
//     // IntPointerTest b = a;

//     // cout << "ref count = " << a << endl;
//     // IntPointerTest c = b;
//     // cout << "ref count = " << a << endl;

//     return 0;
// }