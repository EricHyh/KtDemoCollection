package com.hyh.feeds

import java.util.*

class TTT {

    lateinit var factory1: EventChannelFactory
    lateinit var factory2: ClickAndDeleteEventFactory<Int, String>

    fun test() {
        val xxx = factory1.asTyped<XXX>()
        val createEventChannelFactory = createEventChannelFactory<XXX>()
    }
}

interface XXX : EventChannelFactory {
    fun xxx()
}