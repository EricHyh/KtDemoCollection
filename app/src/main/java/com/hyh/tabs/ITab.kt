package com.hyh.tabs

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/20
 */
interface ITab

interface ITabProvider<Tab : ITab> {

    val tabCount: Int

    fun createTab(position: Int): Tab

    fun getTabTitle(position: Int): CharSequence?

    fun getTabTokens(): List<Any>

    fun getTabToken(position: Int): Any

    fun getCurrentPosition(tabToken: Any): Int

    fun isTabNeedCache(position: Int): Boolean

}