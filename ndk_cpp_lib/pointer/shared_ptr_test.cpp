#include <stdio.h>
#include <string>
#include <iostream>
#include <memory>
#include <functional>

using namespace std;

class SharedPtrTest
{
private:
    /* data */
public:
    SharedPtrTest(/* args */)
    {
        cout << "SharedPtrTest: " << this << endl;
    };
    ~SharedPtrTest()
    {
        cout << "~SharedPtrTest: " << this << endl;
    };
};

namespace shared_ptr_test
{

    class Parent; // Parent类的前置声明

    class Child
    {
    public:
        Child() { cout << "Child" << endl; }
        ~Child() { cout << "~Child" << endl; }

        shared_ptr<Parent> parent;
    };

    class Parent
    {
    public:
        Parent() { cout << "Parent" << endl; }
        ~Parent()
        {
            cout << "~Parent" << endl;
            cout << "ref child:" << child.use_count() << endl;
        }

        shared_ptr<Child> child;
    };

    extern void test1()
    {
        shared_ptr<int> sp = make_shared<int>(10);
        cout << "sp - 1: " << *sp << endl;
        cout << "sp - 1: " << &sp << endl;

        *sp = 20;
        cout << "sp - 2: " << *sp << endl;
        cout << "sp - 2: " << &sp << endl;

        shared_ptr<int> sp2 = make_shared<int>(100);
        sp = sp2;
        cout << "sp - 3: " << *sp << endl;
        cout << "sp - 3: " << &sp << endl;
        cout << "sp - 3: " << &sp2 << endl;
    }

    extern void test2()
    {
        SharedPtrTest *spt = new SharedPtrTest();
        shared_ptr<SharedPtrTest> p1 = make_shared<SharedPtrTest>();
        cout << "1 ref:" << p1.use_count() << endl;
        {
            shared_ptr<SharedPtrTest> p2 = p1;
            cout << "2 ref:" << p1.use_count() << endl;
        }
        cout << "3 ref:" << p1.use_count() << endl;
    }

    extern void test3()
    {
        cout << "test start" << endl;

        SharedPtrTest *p = new SharedPtrTest();
        shared_ptr<SharedPtrTest> p1(p);
        shared_ptr<SharedPtrTest> p2 = make_shared<SharedPtrTest>();

        cout << "p1 ref = " << p1.use_count() << endl;
        cout << "p2 ref = " << p2.use_count() << endl;
        p1 = p2;
        cout << "p1 ref = " << p1.use_count() << endl;
        cout << "p2 ref = " << p2.use_count() << endl;

        cout << "test end" << endl;
    }

    extern void test4()
    {
        SharedPtrTest *p1 = new SharedPtrTest();
        shared_ptr<SharedPtrTest> p2(p1);
        SharedPtrTest *p3 = new SharedPtrTest();
        p2.reset();
        cout << "reset() p2: " << p2 << endl;
        p2.reset(p3);
        cout << "reset(p3) p2: " << p2 << endl;
    }

    extern void test()
    {
        shared_ptr<Child> child = make_shared<Child>();
        shared_ptr<Parent> parent = make_shared<Parent>();

        child->parent = parent;
        parent->child = child;

        cout << child->parent.use_count() << endl;
        cout << parent->child.use_count() << endl;

        child->parent.reset();

        cout << child->parent.use_count() << endl;
        cout << parent.use_count() << endl;
        cout << parent->child.use_count() << endl;
        cout << child.use_count() << endl;
    }

}