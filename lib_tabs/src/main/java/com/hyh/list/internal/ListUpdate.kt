package com.hyh.list.internal

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.hyh.list.ItemData

sealed class UpdateOperate {

    data class OnChanged(val position: Int, val count: Int, val payload: Any? = null) : UpdateOperate()
    data class OnMoved(val fromPosition: Int, val toPosition: Int) : UpdateOperate()
    data class OnInserted(val position: Int, val count: Int) : UpdateOperate()
    data class OnRemoved(val position: Int, val count: Int) : UpdateOperate()

}

data class OnElementChanged<E>(val oldElement: E, val newElement: E, val payload: Any?)

object ListUpdate {

    fun <E> calculateDiff(oldList: MutableList<E>, newList: MutableList<E>, elementDiff: IElementDiff<E>): UpdateResult<E> {
        val list = mutableListOf<E>()
        list.addAll(oldList)
        val diffResult = DiffUtil.calculateDiff(DiffCallbackImpl(oldList, newList, elementDiff))
        val operates = mutableListOf<UpdateOperate>()
        val elementChanges = mutableListOf<OnElementChanged<E>>()
        diffResult.dispatchUpdatesTo(object : ListUpdateCallback {

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                operates.add(UpdateOperate.OnChanged(position, count, payload))
                for (index in position until (position + count)) {
                    val oldElement = oldList[index]
                    val indexOfNewList = indexOf(elementDiff, newList, oldElement)
                    val newElement = newList[indexOfNewList]
                    elementChanges.add(OnElementChanged(oldElement, newElement, payload))
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                operates.add(UpdateOperate.OnMoved(fromPosition, toPosition))
            }

            override fun onInserted(position: Int, count: Int) {
                operates.add(UpdateOperate.OnInserted(position, count))
            }

            override fun onRemoved(position: Int, count: Int) {
                operates.add(UpdateOperate.OnRemoved(position, count))
            }
        })
        return UpdateResult(list, operates, elementChanges)
    }

    private fun <E> indexOf(elementDiff: IElementDiff<E>, list: List<E>, e: E): Int {
        if (e == null) {
            for (i in list.indices) {
                if (list[i] == null) return i
            }
        } else {
            for (i in list.indices) {
                if (elementDiff.elementEquals(e, list[i])) return i
            }
        }
        return -1
    }

    private fun <E> IElementDiff<E>.elementEquals(e1: E, e2: E): Boolean {
        if (e1 === e2) return true
        return areItemsTheSame(e1, e2)
    }

    class UpdateResult<E>(
        val list: MutableList<E>,
        val operates: MutableList<UpdateOperate>,
        val elementChanges: MutableList<OnElementChanged<E>>
    )
}

class DiffCallbackImpl<E>(
    private val oldList: MutableList<E>,
    private val newList: MutableList<E>,
    private val elementDiff: IElementDiff<E>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return elementDiff.areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return elementDiff.areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return elementDiff.getChangePayload(oldList[oldItemPosition], newList[newItemPosition])
    }
}

interface IElementDiff<E> {

    fun areItemsTheSame(oldElement: E, newElement: E): Boolean
    fun areContentsTheSame(oldElement: E, newElement: E): Boolean
    fun getChangePayload(oldElement: E, newElement: E): Any?


    class ItemDataDiff : IElementDiff<ItemData> {

        override fun areItemsTheSame(oldElement: ItemData, newElement: ItemData): Boolean {
            return oldElement.areItemsTheSame(newElement)
        }

        override fun areContentsTheSame(oldElement: ItemData, newElement: ItemData): Boolean {
            return oldElement.areContentsTheSame(newElement)
        }

        override fun getChangePayload(oldElement: ItemData, newElement: ItemData): Any? {
            return oldElement.getChangePayload(newElement)
        }
    }

    class AnyDiff<E> : IElementDiff<E> {

        override fun areItemsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun areContentsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun getChangePayload(oldElement: E, newElement: E): Any? = null
    }
}
