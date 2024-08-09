#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>
// #include <cassert>

using namespace std;

static int static_num = 100;

namespace
{
    static void staticFunc()
    {
        cout << static_num << endl;
    }
}

namespace static_test
{

    int &counter()
    {
        static int count = 0; // 只在第一次调用函数时初始化
        count++;
        cout << count << endl;
        return count;
    }

    void test1()
    {
        counter();
        counter();
        int &count = counter();
        count++;
        counter();
    }

    class StaticTest
    {

    public:
        static int static_num;
        int num;
        StaticTest(/* args */)
        {
            cout << "StaticTest: " << this << ", static_num: " << ++static_num << endl;
        }
        ~StaticTest()
        {
            cout << "~StaticTest: " << this << endl;
        }

        int &getNum()
        {
            static int otherNum = 100;
            num = otherNum;
            otherNum++;
            return num;
        }

        static int &getStaticNum()
        {
            static int otherNum = 100;
            static_num = otherNum;
            otherNum++;
            return static_num;
        }
    };

    int StaticTest::static_num = 0;

    void test2()
    {
        StaticTest *st1 = new StaticTest();
        StaticTest *st2 = new StaticTest();
        cout << "st1->num" << endl;
        delete st1;
        delete st2;
    }

    void test3()
    {
        StaticTest::static_num++;
        int num1 = StaticTest::getStaticNum();
        num1++;
        cout << StaticTest::getStaticNum() << endl;

        int &num2 = StaticTest::getStaticNum();
        num2++;
        cout << StaticTest::getStaticNum() << endl;
    }

    void test4()
    {
        shared_ptr<StaticTest> st = make_shared<StaticTest>();
        cout << st->getNum() << endl;
        cout << st->getNum() << endl;
    }

    void test5()
    {
        static_num++;
        staticFunc();
    }

    class SingletonTest
    {
    private:
        /* data */
    public:
        static SingletonTest &getInstance()
        {
            static SingletonTest instance;
            return instance;
        }
        ~SingletonTest()
        {
            cout << "~SingletonTest: " << this << endl;
        }

    private:
        SingletonTest()
        {
            cout << "SingletonTest: " << this << endl;
        }

        SingletonTest(const SingletonTest &) = delete;
        SingletonTest &operator=(const SingletonTest &) = delete;
    };

    const int getIntBytes()
    {
        return 4;
    }

    void test()
    {
        SingletonTest &instance1 = SingletonTest::getInstance();
        cout << &instance1 << endl;

        static_assert(sizeof(int) == 4, "Int must be 4 bytes");
    }

}
