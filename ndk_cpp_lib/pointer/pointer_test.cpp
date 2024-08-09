

#include <stdio.h>
#include <string>
#include <iostream>

using namespace std;

const int MAX = 4;

// int main()
// {
//     // int a = 1;
//     // std::cout << a << std::endl;
//     // std::cout << &a << std::endl;

//     // int *ap;
//     // ap = &a;

//     // std::cout << &ap << std::endl;
//     // std::cout << ap << std::endl;
//     // std::cout << *ap << std::endl;

//     // *ap = 2;
//     // std::cout << a << std::endl;

//     // a = 3;
//     // std::cout << *ap << std::endl;

//     // int b = 10;

//     // ap = &b;
//     // std::cout << *ap << std::endl;
//     // *ap = 11;
//     // std::cout << b << std::endl; // 变量 b 的值为11

//     // char c = 'c';
//     // ap = reinterpret_cast<int *>(&c);

//     // std::cout << c << std::endl;
//     // std::cout << *ap << std::endl;
//     // *ap = 100;
//     // std::cout << c << std::endl;
//     // std::cout << *ap << std::endl;

//     // int arr[5] = {5, 4, 3, 2, 1};
//     // int *arrp = arr;

//     // std::cout << &arrp << std::endl;
//     // std::cout << arrp << std::endl;

//     // std::cout << arrp[1] << std::endl;
//     // std::cout << &*arrp << std::endl;

//     // std::cout << sizeof(arrp) << std::endl;

//     // int *ptr = NULL;
//     // ptr = nullptr;

//     // if (ptr)
//     // {
//     //     std::cout << "if" << std::endl;
//     // }
//     // else
//     // {
//     //     std::cout << "else" << std::endl;
//     // }

//     // std::cout << ptr << std::endl;

//     // int a = 0;
//     // ptr = &a;
//     // std::cout << ptr << std::endl;

//     // const char *first = "Zara Ali";

//     // std::cout << "&first = " << &first << std::endl;

//     // const char *names[MAX] = {
//     //     first,
//     //     "Hina Ali",
//     //     "Nuha Ali",
//     //     "Sara Ali",
//     // };

//     // // auto name = &names[0];
//     // // std::cout << "name = " << name << std::endl;

//     // first = "123";

//     // std::cout << "first = " << first << std::endl;

//     // for (int i = 0; i < MAX; i++)
//     // {
//     //     cout << "Value of names[" << i << "] = ";
//     //     cout << names[i] << endl;
//     // }

//     // int a = 101;
//     // int b = 102;
//     // int c = 103;
//     // int d = 104;

//     // int *nums[MAX] = {
//     //     &a,
//     //     &b,
//     //     &c,
//     //     &d,
//     // };

//     // a = 100;

//     // cout << a << endl;

//     // for (int i = 0; i < MAX; i++)
//     // {
//     //     cout << "Value of nums[" << i << "] = ";
//     //     cout << *nums[i] << endl;
//     // }

//     return 0;
// }

// int main()
// {

//     char c = 'a';
//     char *cp = &c;

//     cout << c << endl;
//     cout << &c << endl;

//     *cp = 'b';

//     cout << c << endl;
//     cout << &c << endl;

//     void *vp = static_cast<void *>(cp);

//     cout << vp << endl;

//     return 0;
// }

// int *getIntPtr()
// {
//     static int a = 1;
//     return &a;
// }

// int *getIntPtr2()
// {
//     int *a = new int(10);
//     return a;
// }

// int b = 10;

// int *updateValue(int *intPtr)
// {
//     int *intPtr2 = intPtr;
//     *intPtr2 = 1000;
//     cout << intPtr2 << endl;
//     return intPtr2;
// }

// int main()
// {

//     int *intPtr = getIntPtr2();

//     int *intPtr2 = updateValue(intPtr);

//     cout << intPtr << endl;
//     cout << *intPtr << endl;

//     delete intPtr;


//     // int a = 2;
//     // int *intPtr = &a;

//     // int *intPtr2 = updateValue(intPtr);

//     // cout << intPtr << endl;
//     // cout << *intPtr << endl;

//     // cout << intPtr2 << endl;
//     // cout << *intPtr2 << endl;

//     return 0;
// }