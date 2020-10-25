package com.hyh.adapter

interface ItemViewTypeProvider<T> {

    fun getItemViewType(adapter: ChildAdapter<T>, data: T, position: Int): Int

    fun getChildAdapter(type: Int): ChildAdapter<T>

}