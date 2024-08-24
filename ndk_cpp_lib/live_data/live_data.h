#ifndef LIVE_DATA_H
#define LIVE_DATA_H

#include <iostream>
#include <vector>
#include <functional>
#include <algorithm>

template <typename T>
class LiveData
{
private:
    /* data */
    T value;

public:
    explicit LiveData(const T &initial);
    ~LiveData() = default;

    T getValue();
};

template <typename T>
LiveData<T>::LiveData(const T &initial) : value(initial) {}

// template <typename T>
// LiveData<T>::LiveData(const T &initial)
// {
//     this->value = initial
// }

template <typename T>
T LiveData<T>::getValue()
{
    return this->value;
}

// template <typename T>
// class LiveData {
// private:
//     T value;

// public:
//     explicit LiveData(const T& value) : value(value) {}
//     LiveData(const LiveData&) = default;
//     LiveData(LiveData&&) = default;
//     LiveData& operator=(const LiveData&) = default;
//     LiveData& operator=(LiveData&&) = default;
//     ~LiveData() = default;

//     T getValue() const { return value; }
//     void setValue(const T& newValue) { value = newValue; }
// };

// template<typename T>
// class LiveData {
// private:
//     T value;
//     std::vector<std::function<void(const T&)>> observers;

// public:
//     LiveData() = default;
//     explicit LiveData(const T& initial) : value(initial) {}

//     // 设置新值并通知所有观察者
//     void setValue(const T& newValue) {
//         if (value != newValue) {
//             value = newValue;
//             notifyObservers();
//         }
//     }

//     // 获取当前值
//     T getValue() const {
//         return value;
//     }

//     // 添加观察者
//     void observe(const std::function<void(const T&)>& observer) {
//         observers.push_back(observer);
//         // 立即通知新的观察者当前值
//         observer(value);
//     }

//     // 移除观察者
//     void removeObserver(const std::function<void(const T&)>& observer) {
//         observers.erase(
//             std::remove_if(observers.begin(), observers.end(),
//                 [&](const auto& obs) { return &obs == &observer; }),
//             observers.end()
//         );
//     }

// private:
//     // 通知所有观察者
//     void notifyObservers() {
//         for (const auto& observer : observers) {
//             observer(value);
//         }
//     }
// };

#endif