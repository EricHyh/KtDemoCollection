package com.hyh.feeds

import android.view.View

interface IItemData {

    fun getLayoutResId(): Int

    fun onBindViewHolder(itemView: View)

}