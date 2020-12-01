package com.hyh.feeds

/**
 * TODO: Add Description
 *
 * 1.可读性
 * 2.拓展性
 * 3.可用性
 * 4.约束性
 * <br>
 *
 *
 * @author eriche
 * @data 2020/12/1
 */
class EventChannel {

    fun registerEvent(type: Int) {

    }

    //Type的关联性太弱，可读性弱
    //容易出现定义模糊的Type，约束性弱
    fun postEvent(type: Int, data: EventData) {

    }
}

interface EventReceiver {

    fun onEvent(type: Int, data: EventData)

}

class EventData(val data: Any? = null) {
    inline fun <reified T> getTypedData(): T? {
        return if (data is T) {
            data as T
        } else {
            null
        }
    }
}

