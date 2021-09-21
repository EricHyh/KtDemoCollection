package com.hyh.paging3demo.list.fragment.item

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.IFlatListItem
import com.hyh.list.decoration.CardPosition
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class BaseCardItem<VH : RecyclerView.ViewHolder>
    : IFlatListItem<VH>() {

    private val lazyItemViewType: Int by lazy {
        findViewHoldType()?.hashCode() ?: this.javaClass.hashCode()
    }

    var isLastPositionItem = false

    /**
     *
     * 持仓列表的所有 itemViewType，统一采用 ViewHolder 类的 hashCode 作为 viewType.
     * 为什么不直接用 layout id，理由如下：
     * 1.layout id 可能会被不同的 ViewHolder 复用
     * 2.直接创建 View 的情况，没有 layout id
     */
    final override fun getItemViewType(): Int {
        return lazyItemViewType
    }

    abstract fun getCardPosition(): CardPosition

    abstract fun getItemOffsets(outRect: Rect)

    /**
     * 查找泛型定义的 [RecyclerView.ViewHolder] 类型
     */
    private fun findViewHoldType(): Type? {
        return findViewHoldType(this.javaClass.genericSuperclass, this.javaClass.superclass)
    }

    private fun findViewHoldType(type: Type?, cls: Class<*>?): Type? {
        if (cls == null) return null
        if (type is ParameterizedType) {
            val viewHoldType = findViewHoldType(type.actualTypeArguments)
            if (viewHoldType != null) {
                return viewHoldType
            }
        }
        return findViewHoldType(cls.genericSuperclass, cls.superclass)
    }


    private fun findViewHoldType(actualTypeArguments: Array<Type>?): Type? {
        if (actualTypeArguments == null) return null
        return actualTypeArguments.find {
            if (it is Class<*>) {
                return@find RecyclerView.ViewHolder::class.java.isAssignableFrom(it)
            }
            return@find false
        }
    }
}