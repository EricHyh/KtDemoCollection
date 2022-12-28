package com.hyh.socketdemo.channel

import java.io.Closeable

/**
 * IO流扩展
 *
 * @author eriche 2022/12/28
 */
object StreamUtils {

    fun closeSafety(vararg closeable: Closeable?) {
        closeable.onEach {
            it.closeSafety()
        }
    }

    fun Closeable?.closeSafety() {
        try {
            this?.close()
        } catch (ignored: Throwable) {
        }
    }
}



