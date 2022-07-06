package com.hyh.list.internal.utils

import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.hyh.Invoke
import com.hyh.list.FlatListItem
import com.hyh.list.ItemsBucket
import com.hyh.list.internal.ListItemWrapper
import java.util.*
import kotlin.collections.ArrayList

/**
 * 与[RecyclerView.Adapter]对应的列表操作
 */
sealed class ListOperate {

    /**
     * 列表全部变化，对应[RecyclerView.Adapter.notifyDataSetChanged]
     */
    object OnAllChanged : ListOperate()

    /**
     * 列表部分变化，对应[RecyclerView.Adapter.notifyItemRangeChanged]
     *
     * @property positionStart 其实位置
     * @property count 变化的数目
     * @property payload 变化的内容
     */
    data class OnChanged(val positionStart: Int, val count: Int, val payload: Any? = null) : ListOperate()

    /**
     * 列表元素移动，对应[RecyclerView.Adapter.notifyItemMoved]
     *
     * @property fromPosition 原始位置
     * @property toPosition 目标位置
     */
    data class OnMoved(val fromPosition: Int, val toPosition: Int) : ListOperate()

    /**
     * 列表插入元素，对应[RecyclerView.Adapter.notifyItemRangeInserted]
     *
     * @property positionStart 插入起始位置
     * @property count 插入数目
     */
    data class OnInserted(val positionStart: Int, val count: Int) : ListOperate()

    /**
     * 列表删除元素，对应[RecyclerView.Adapter.notifyItemRangeRemoved]
     *
     * @property positionStart 插入起始位置
     * @property count 删除数目
     */
    data class OnRemoved(val positionStart: Int, val count: Int) : ListOperate()

}

/**
 * 对比两个列表，产生的列表元素增删改操作
 *
 * @param E 元素泛型
 * @property addedElements 增加的元素
 * @property removedElements 删除的元素
 * @property changedElements 改变的元素
 */
data class ElementOperates<E>(
    val addedElements: List<E>,
    val removedElements: List<E>,
    val changedElements: List<Triple<E, E, Any?>>
)

/**
 * 列表比对工具类
 */
object ListUpdate {

    private const val TAG = "ListUpdate"

    /**
     * 计算两个列表的变化
     *
     * @param E 元素泛型
     * @param oldList 旧元素列表
     * @param newList 新元素列表
     * @param elementDiff 元素比对接口
     * @return 比对结果
     */
    fun <E> calculateDiff(oldList: List<E>?, newList: List<E>, elementDiff: IElementDiff<E>): UpdateResult<E> {
        if (oldList == null) {
            return UpdateResult(
                resultList = ArrayList(newList),
                listOperates = listOf(ListOperate.OnAllChanged),
                elementOperates = ElementOperates(newList, emptyList(), emptyList())
            )
        }


        val list = mutableListOf<ElementStub<E>>()
        list.addAll(oldList.map { ElementStub(it) })

        val contentsNotSameMap: IdentityHashMap<E, E> = IdentityHashMap()
        val diffResult = SimpleDiffUtil.calculateDiff(DiffCallbackImpl(oldList, newList, elementDiff, contentsNotSameMap))
        val operates = mutableListOf<ListOperate>()

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
                        if (elementDiff.isSupportUpdate(oldElement, newElement)) {
                            changedElements.add(Triple(oldElement, newElement, payload))
                        } else {
                            elementStub.element = newElement
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
                    removedElements.add(element)
                }
            }
        })

        list.forEachIndexed { index, elementStub ->
            if (elementStub.element == null) {
                elementStub.element = newList[index]
                addedElements.add(elementStub.element!!)
            }
        }

        elementChangeBuilders.forEach {
            it()
        }

        return UpdateResult(
            list.map { it.element!! },
            operates,
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

    fun <E> remove(list: MutableList<E>, fromIndex: Int, count: Int): List<E> {
        if (fromIndex !in list.indices) return emptyList()
        if ((fromIndex + count - 1) !in list.indices) return emptyList()
        val removed = mutableListOf<E>()
        while (removed.size < count) {
            removed.add(list.removeAt(fromIndex))
        }
        return removed
    }

    fun <E> calculateDiff(
        old: Collection<E>?,
        new: Collection<E>?,
        added: MutableList<E>,
        removed: MutableList<E>
    ) {
        added.clear()
        removed.clear()
        if (old.isNullOrEmpty()) {
            added.addAll(new ?: emptyList())
            return
        }
        if (new.isNullOrEmpty()) {
            removed.addAll(old)
            return
        }

        added.addAll(new)
        added.removeAll(old)

        removed.addAll(old)
        removed.removeAll(new)
    }

    fun <E> calculateDiff(
        old: Collection<E>?,
        new: Collection<E>?
    ): Pair<Collection<E>, Collection<E>> {
        val added = mutableListOf<E>()
        val removed = mutableListOf<E>()
        calculateDiff(old, new, added, removed)
        return Pair(added, removed)
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

    /**
     * 两个列表比对后的结果
     *
     * @param E 元素泛型
     * @property resultList 比对后的最终结果列表
     * @property listOperates 比对过程中对列表进行的一些操作
     * @property elementOperates 元素的增删改结果
     */
    class UpdateResult<E>(
        val resultList: List<E>,
        val listOperates: List<ListOperate>,
        val elementOperates: ElementOperates<E>
    )
}

class DiffCallbackImpl<E>(
    private val oldList: List<E>,
    private val newList: List<E>,
    private val elementDiff: IElementDiff<E>,
    private val contentsNotSameMap: IdentityHashMap<E, E>
) : SimpleDiffUtil.Callback() {

    /*override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size*/

    override val oldListSize: Int
        get() = oldList.size

    override val newListSize: Int
        get() = newList.size

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

/**
 * 元素比对接口
 *
 * @param E 元素泛型
 */
interface IElementDiff<E> {

    /**
     * 元素是否支持更新，如果支持更新，那么[areItemsTheSame]为true时，旧元素会被保留
     *
     * @param oldElement 旧元素
     * @param newElement 新元素
     * @return
     */
    fun isSupportUpdate(oldElement: E, newElement: E): Boolean

    /**
     * 是否为同一个元素
     *
     * @param oldElement 旧元素
     * @param newElement 新元素
     * @return
     */
    fun areItemsTheSame(oldElement: E, newElement: E): Boolean

    /**
     * 同一个元素的内容是否相同
     *
     * @param oldElement 旧元素
     * @param newElement 新元素
     * @return
     */
    fun areContentsTheSame(oldElement: E, newElement: E): Boolean

    /**
     * 同一个元素如果内容不相同，那么在这里返回变化的部分
     *
     * @param oldElement 旧元素
     * @param newElement 新元素
     * @return
     */
    fun getChangePayload(oldElement: E, newElement: E): Any?


    class ItemDataDiff : IElementDiff<FlatListItem> {

        override fun isSupportUpdate(oldElement: FlatListItem, newElement: FlatListItem): Boolean {
            return oldElement.isSupportUpdateItem()
        }

        override fun areItemsTheSame(oldElement: FlatListItem, newElement: FlatListItem): Boolean {
            if (oldElement.getItemViewType() != newElement.getItemViewType()) return false
            return oldElement.areItemsTheSame(newElement)
        }

        override fun areContentsTheSame(oldElement: FlatListItem, newElement: FlatListItem): Boolean {
            return oldElement.areContentsTheSame(newElement)
        }

        override fun getChangePayload(oldElement: FlatListItem, newElement: FlatListItem): Any? {
            return oldElement.getChangePayload(newElement)
        }
    }

    class ItemDataWrapperDiff : IElementDiff<ListItemWrapper> {

        override fun isSupportUpdate(oldElement: ListItemWrapper, newElement: ListItemWrapper): Boolean {
            return oldElement.isSupportUpdateItemData(newElement)
        }

        override fun areItemsTheSame(oldElement: ListItemWrapper, newElement: ListItemWrapper): Boolean {
            return oldElement.areItemsTheSame(newElement)
        }

        override fun areContentsTheSame(oldElement: ListItemWrapper, newElement: ListItemWrapper): Boolean {
            return oldElement.areContentsTheSame(newElement)
        }

        override fun getChangePayload(oldElement: ListItemWrapper, newElement: ListItemWrapper): Any? {
            return oldElement.getChangePayload(newElement)
        }
    }

    class BucketDiff : IElementDiff<ItemsBucket> {

        override fun isSupportUpdate(
            oldElement: ItemsBucket,
            newElement: ItemsBucket
        ): Boolean = true

        override fun areItemsTheSame(oldElement: ItemsBucket, newElement: ItemsBucket): Boolean {
            return oldElement.bucketId == newElement.bucketId
        }

        override fun areContentsTheSame(
            oldElement: ItemsBucket,
            newElement: ItemsBucket
        ): Boolean {
            return oldElement.itemsToken == newElement.itemsToken
        }

        override fun getChangePayload(oldElement: ItemsBucket, newElement: ItemsBucket): Any? {
            return newElement.itemsToken
        }
    }

    class AnyDiff<E> : IElementDiff<E> {

        override fun isSupportUpdate(oldElement: E, newElement: E): Boolean = true

        override fun areItemsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement?.hashCode() == newElement?.hashCode()
        }

        override fun areContentsTheSame(oldElement: E, newElement: E): Boolean {
            return oldElement == newElement
        }

        override fun getChangePayload(oldElement: E, newElement: E): Any? = null
    }
}
