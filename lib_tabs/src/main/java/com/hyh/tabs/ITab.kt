package com.hyh.tabs

/**
 * 用于标识Tab数据的接口
 *
 * @author eriche
 * @data 2021/5/20
 */
interface ITab

class TabInfo<Tab : ITab>(
    val lazyTab: Lazy<Tab>,
    val tabToken: Any,
    val tabTitle: CharSequence,
    val isTabNeedCache: Boolean = true
) {

    override fun hashCode(): Int {
        return tabToken.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TabInfo<*>) return false
        return tabToken == other.tabToken
    }
}