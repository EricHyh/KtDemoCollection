package com.hyh.paging3demo.widget.horizontal.internal

/**
 * 可滑动控件接口描述
 *
 * @author eriche 2021/12/28
 */
interface Scrollable<T : IScrollData> {

    fun getScrollData(): T

    fun scrollTo(t: T)

    fun resetScroll()

    fun stopScroll()

}


interface IScrollData : Cloneable {

    public override fun clone(): Any

    fun copy(other: IScrollData)

}


