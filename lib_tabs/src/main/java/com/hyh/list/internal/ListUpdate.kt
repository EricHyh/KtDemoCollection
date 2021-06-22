package com.hyh.list.internal

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.list.ItemData
import java.util.*
import kotlin.collections.ArrayList

sealed class ListOperate {

    object OnAllChanged : ListOperate()
    data class OnChanged(val positionStart: Int, val count: Int, val payload: Any? = null) : ListOperate()
    data class OnMoved(val fromPosition: Int, val toPosition: Int) : ListOperate()
    data class OnInserted(val positionStart: Int, val count: Int) : ListOperate()
    data class OnRemoved(val positionStart: Int, val count: Int) : ListOperate()

}


sealed class ElementOperate<E> {

    data class Added<E>(val element: E) : ElementOperate<E>()
    data class Changed<E>(val oldElement: E, val newElement: E, val payload: Any?) : ElementOperate<E>()
    data class Removed<E>(val element: E) : ElementOperate<E>()

}


object ListUpdate {

    fun <E> replaceAll(oldList: List<E>?, newList: List<E>): UpdateResult<E> {
        if (oldList == null) {
            return UpdateResult(
                list = ArrayList(newList),
                listOperates = listOf(ListOperate.OnAllChanged),
                elementOperates = newList.map {
                    ElementOperate.Added(it)
                }
            )
        }

        val listOperates: MutableList<ListOperate> = mutableListOf()
        listOperates.add(ListOperate.OnRemoved(0, oldList.size))
        listOperates.add(ListOperate.OnInserted(0, newList.size))

        val elementOperates = oldList.map { ElementOperate.Removed(it) }
        return UpdateResult(newList, listOperates, elementOperates)
    }

    fun <E> calculateDiff(oldList: List<E>?, newList: List<E>, elementDiff: IElementDiff<E>): UpdateResult<E> {
        if (oldList == null) {
            return UpdateResult(
                list = ArrayList(newList),
                listOperates = listOf(ListOperate.OnAllChanged),
                elementOperates = newList.map {
                    ElementOperate.Added(it)
                }
            )
        }

        val list = mutableListOf<ElementStub<E>>()
        list.addAll(oldList.map { ElementStub(it) })

        val contentsNotSameMap: IdentityHashMap<E, E> = IdentityHashMap()
        val diffResult = DiffUtil.calculateDiff(DiffCallbackImpl(oldList, newList, elementDiff, contentsNotSameMap))
        val operates = mutableListOf<ListOperate>()
        val elementOperates = mutableListOf<ElementOperate<E>>()

        val elementChangeBuilders = mutableListOf<Invoke>()

        diffResult.dispatchUpdatesTo(object : ListUpdateCallback {

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                operates.add(ListOperate.OnChanged(position, count, payload))
                val listSnapshot = ArrayList(list)
                for (index in position until (position + count)) {
                    elementChangeBuilders.add {
                        val elementStub = listSnapshot[index]
                        val oldElement = elementStub.element!!
                        val newElement = contentsNotSameMap[oldElement]!!
                        if (elementDiff.isSupportUpdateItemData(oldElement)) {
                            elementOperates.add(ElementOperate.Changed(oldElement, newElement, payload))
                        } else {
                            elementStub.element = newElement
                            elementOperates.add(ElementOperate.Removed(oldElement))
                            elementOperates.add(ElementOperate.Added(newElement))
                        }
                    }
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                operates.add(ListOperate.OnMoved(fromPosition, toPosition))
                move(list, fromPosition, toPosition)

            }

            override fun onInserted(position: Int, count: Int) {
                operates.add(ListOperate.OnInserted(position, count))
                for (index in position until (position + count)) {
                    val elementStub = ElementStub<E>()
                    list.add(index, elementStub)
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                operates.add(ListOperate.OnRemoved(position, count))
                val subList = ArrayList(list).subList(position, position + count)
                list.removeAll(subList)
                subList.forEach {
                    val element = it.element ?: return@forEach
                    elementOperates.add(ElementOperate.Removed(element))
                }
            }
        })
        list.forEachIndexed { index, elementStub ->
            if (elementStub.element == null) {
                elementStub.element = newList[index]
                elementOperates.add(ElementOperate.Added(elementStub.element!!))
            }
        }
        elementChangeBuilders.forEach {
            it()
        }

        return UpdateResult(list.map { it.element!! }, operates, elementOperates)
    }

    fun <E> move(list: MutableList<E>, sourceIndex: Int, targetIndex: Int): Boolean {
        if (list.isNullOrEmpty()) return false
        val size = list.size
        if (size <= sourceIndex || size <= targetIndex) return false
        if (sourceIndex == targetIndex) {
            return true
        }
        list.add(targetIndex, list.removeAt(sourceIndex))
        return true
    }

    fun handleItemDataChanges(elementChanges: List<ElementOperate<ItemData>>) {
        elementChanges.forEach {
            when (it) {
                is ElementOperate.Added<ItemData> -> {
                    it.element.delegate.activate()
                }
                is ElementOperate.Changed<ItemData> -> {
                    it.oldElement.delegate.updateItemData(it.newElement)
                }
                is ElementOperate.Removed<ItemData> -> {
                    it.element.delegate.destroy()
                }
            }
        }
    }

    fun handleListOperates(listOperates: List<ListOperate>, adapter: RecyclerView.Adapter<*>) {
        listOperates.forEach {
            when (it) {
                is ListOperate.OnAllChanged -> {
                    adapter.notifyDataSetChanged()
                }
                is ListOperate.OnChanged -> {
                    adapter.notifyItemRangeChanged(it.positionStart, it.count)
                }
                is ListOperate.OnMoved -> {
                    adapter.notifyItemMoved(it.fromPosition, it.toPosition)
                }
                is ListOperate.OnInserted -> {
                    adapter.notifyItemRangeInserted(it.positionStart, it.count)
                }
                is ListOperate.OnRemoved -> {
                    adapter.notifyItemRangeRemoved(it.positionStart, it.count)
                }
            }
        }
    }

    class ElementStub<E>(
        var element: E? = null
    )

    class UpdateResult<E>(
        val list: List<E>,
        val listOperates: List<ListOperate>,
        val elementOperates: List<ElementOperate<E>>
    )
}

class DiffCallbackImpl<E>(
    private val oldList: List<E>,
    private val newList: List<E>,
    private val elementDiff: IElementDiff<E>,
    private val contentsNotSameMap: IdentityHashMap<E, E>,
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return elementDiff.areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldElement = oldList[oldItemPosition]
        val newElement = newList[newItemPosition]
        val areContentsTheSame = elementDiff.areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
        if (!areContentsTheSame) {
            contentsNotSameMap[oldElement] = newElement
        }
        return areContentsTheSame
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return elementDiff.getChangePayload(oldList[oldItemPosition], newList[newItemPosition])
    }
}

interface IElementDiff<E> {

    fun isSupportUpdateItemData(element: E): Boolean
    fun areItemsTheSame(oldElement: E, newElement: E): Boolean
    fun areContentsTheSame(oldElement: E, newElement: E): Boolean
    fun getChangePayload(oldElement: E, newElement: E): Any?


    class ItemDataDiff : IElementDiff<ItemData> {

        override fun isSupportUpdateItemData(element: ItemData): Boolean {
            return element.isSupportUpdateItemData()
        }

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

        override fun isSupportUpdateItemData(element: E): Boolean = true

        override fun areItemsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun areContentsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun getChangePayload(oldElement: E, newElement: E): Any? = null
    }
}
