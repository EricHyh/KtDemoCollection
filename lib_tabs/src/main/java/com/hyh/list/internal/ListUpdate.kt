package com.hyh.list.internal

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.list.ItemData
import com.hyh.list.ItemSource
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


data class ElementOperates<E>(
    val addedElements: List<E>,
    val removedElements: List<E>,
    val changedElements: List<Triple<E, E, Any?>>
)


object ListUpdate {

    private const val TAG = "ListUpdate"

    fun <E> calculateDiff(oldList: List<E>?, newList: List<E>, elementDiff: IElementDiff<E>): UpdateResult<E> {
        if (oldList == null) {
            return UpdateResult(
                resultList = ArrayList(newList),
                listOperates = listOf(ListOperate.OnAllChanged),
                elementOperates = newList.map {
                    ElementOperate.Added(it)
                },
                newElementOperates = ElementOperates(newList, emptyList(), emptyList())
            )
        }


        val list = mutableListOf<ElementStub<E>>()
        list.addAll(oldList.map { ElementStub(it) })

        val contentsNotSameMap: IdentityHashMap<E, E> = IdentityHashMap()
        val diffResult = DiffUtil.calculateDiff(DiffCallbackImpl(oldList, newList, elementDiff, contentsNotSameMap))
        val operates = mutableListOf<ListOperate>()

        val elementOperates = mutableListOf<ElementOperate<E>>()

        val addedElements: MutableList<E> = mutableListOf()
        val removedElements: MutableList<E> = mutableListOf()
        val changedElements: MutableList<Triple<E, E, Any?>> = mutableListOf()


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
                        if (elementDiff.isSupportUpdateItemData(oldElement, newElement)) {
                            elementOperates.add(ElementOperate.Changed(oldElement, newElement, payload))

                            changedElements.add(Triple(oldElement, newElement, payload))
                        } else {
                            elementStub.element = newElement
                            elementOperates.add(ElementOperate.Removed(oldElement))
                            elementOperates.add(ElementOperate.Added(newElement))

                            addedElements.add(newElement)
                            removedElements.add(oldElement)
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

                    removedElements.add(element)
                }
            }
        })

        list.forEachIndexed { index, elementStub ->
            if (elementStub.element == null) {
                elementStub.element = newList[index]
                elementOperates.add(ElementOperate.Added(elementStub.element!!))

                addedElements.add(elementStub.element!!)
            }
        }

        elementChangeBuilders.forEach {
            it()
        }

        return UpdateResult(
            list.map { it.element!! },
            operates,
            elementOperates,
            ElementOperates(addedElements, removedElements, changedElements)
        )
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
                    it.element.delegate.onActivated()
                }
                is ElementOperate.Changed<ItemData> -> {
                    it.oldElement.delegate.updateItemData(it.newElement, it.payload)
                }
                is ElementOperate.Removed<ItemData> -> {
                    it.element.delegate.onDetached()
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
                    adapter.notifyItemRangeChanged(it.positionStart, it.count, it.payload)
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
        val resultList: List<E>,
        val listOperates: List<ListOperate>,
        val elementOperates: List<ElementOperate<E>>,
        val newElementOperates: ElementOperates<E>
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

    fun isSupportUpdateItemData(oldElement: E, newElement: E): Boolean
    fun areItemsTheSame(oldElement: E, newElement: E): Boolean
    fun areContentsTheSame(oldElement: E, newElement: E): Boolean
    fun getChangePayload(oldElement: E, newElement: E): Any?


    class ItemDataDiff : IElementDiff<ItemData> {

        override fun isSupportUpdateItemData(oldElement: ItemData, newElement: ItemData): Boolean {
            return oldElement.isSupportUpdateItemData()
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

    class ItemDataWrapperDiff : IElementDiff<ItemDataWrapper> {

        override fun isSupportUpdateItemData(oldElement: ItemDataWrapper, newElement: ItemDataWrapper): Boolean {
            return oldElement.isSupportUpdateItemData(newElement)
        }

        override fun areItemsTheSame(oldElement: ItemDataWrapper, newElement: ItemDataWrapper): Boolean {
            return oldElement.areItemsTheSame(newElement)
        }

        override fun areContentsTheSame(oldElement: ItemDataWrapper, newElement: ItemDataWrapper): Boolean {
            return oldElement.areContentsTheSame(newElement)
        }

        override fun getChangePayload(oldElement: ItemDataWrapper, newElement: ItemDataWrapper): Any? {
            return oldElement.getChangePayload(newElement)
        }
    }

    class BucketDiff : IElementDiff<ItemSource.ItemsBucket> {

        override fun isSupportUpdateItemData(oldElement: ItemSource.ItemsBucket, newElement: ItemSource.ItemsBucket): Boolean = true

        override fun areItemsTheSame(oldElement: ItemSource.ItemsBucket, newElement: ItemSource.ItemsBucket): Boolean {
            return oldElement.bucketId == newElement.bucketId
        }

        override fun areContentsTheSame(oldElement: ItemSource.ItemsBucket, newElement: ItemSource.ItemsBucket): Boolean {
            return oldElement.itemsToken == newElement.itemsToken
        }

        override fun getChangePayload(oldElement: ItemSource.ItemsBucket, newElement: ItemSource.ItemsBucket): Any? {
            return newElement.itemsToken
        }
    }

    class AnyDiff<E> : IElementDiff<E> {

        override fun isSupportUpdateItemData(oldElement: E, newElement: E): Boolean = true

        override fun areItemsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun areContentsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun getChangePayload(oldElement: E, newElement: E): Any? = null
    }
}
