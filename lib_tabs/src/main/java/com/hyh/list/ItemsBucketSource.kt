package com.hyh.list

import com.hyh.Invoke
import com.hyh.list.internal.*
import com.hyh.list.internal.utils.IElementDiff
import com.hyh.list.internal.utils.ListUpdate
import java.util.*
import kotlin.collections.LinkedHashMap

abstract class ItemsBucketSource<Param> : ItemSource<Param, ListItemWrapper>() {

    companion object {
        const val DEFAULT_ITEMS_BUCKET_ID = -1
        val DEFAULT_ITEMS_TOKEN = Any()
    }

    protected val storage: ItemsBucketStorage = ItemsBucketStorage()

    private var _itemsBucketIds: List<Int> = listOf(DEFAULT_ITEMS_BUCKET_ID)
    protected val itemsBucketIds: List<Int>
        get() = _itemsBucketIds

    protected fun registerItemsBucketIds(itemsBucketIds: List<Int>) {
        this._itemsBucketIds = itemsBucketIds
    }

    override fun getElementDiff(): IElementDiff<FlatListItem> {
        return IElementDiff.ItemDataDiff()
    }

    override fun mapItems(items: List<ListItemWrapper>): List<FlatListItem> {
        return items.map { it.item }
    }

    override fun onItemsDisplayed(items: List<FlatListItem>) {
        items.forEach {
            if (!it.delegate.attached) {
                it.delegate.attachedSourceToken = sourceToken
                it.delegate.onItemAttached()
            }
            it.delegate.onItemActivated()
        }
    }

    override fun onItemsChanged(changes: List<Triple<FlatListItem, FlatListItem, Any?>>) {
        changes.forEach {
            it.first.delegate.updateItem(it.second, it.third)
        }
    }

    override fun onItemsRecycled(items: List<FlatListItem>) {
        items.forEach {
            it.delegate.onItemInactivated()
            if (!it.delegate.cached) {
                it.delegate.onItemDetached()
            }
        }
    }

    override suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult<ListItemWrapper> {
        val resultExtra = params.displayedData.resultExtra as? ResultExtra
        val result = getPreShow(params.param, resultExtra?.resultItemsBucketMap)
        return if (result is BucketPreShowResult.Success) {
            val itemsBucketIds = result.itemsBucketIds
            val itemsBucketMap = result.itemsBucketMap
            val itemWrappers = getItemWrappers(itemsBucketIds, itemsBucketMap)
            PreShowResult.Success(itemWrappers, ResultExtra())
        } else {
            PreShowResult.Unused()
        }
    }

    override suspend fun load(params: LoadParams<Param>): LoadResult<ListItemWrapper> {
        val resultExtra = params.displayedData.resultExtra as? ResultExtra
        val result = load(params.param, resultExtra?.resultItemsBucketMap)
        return if (result is BucketLoadResult.Success) {
            val itemsBucketIds = result.itemsBucketIds
            val itemsBucketMap = result.itemsBucketMap
            val itemWrappers = getItemWrappers(itemsBucketIds, itemsBucketMap)
            LoadResult.Success(itemWrappers, ResultExtra())
        } else {
            LoadResult.Error((result as BucketLoadResult.Error).error)
        }
    }

    protected abstract suspend fun getPreShow(
        param: Param,
        displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?
    ): BucketPreShowResult

    protected abstract suspend fun load(
        param: Param,
        displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?
    ): BucketLoadResult

    override fun onProcessResult(
        resultItems: List<FlatListItem>,
        resultExtra: Any?,
        displayedData: SourceDisplayedData
    ) {
        val displayedExtra = displayedData.resultExtra as? ResultExtra
        val newExtra = resultExtra as? ResultExtra
        check(newExtra != null) {
            "$this onProcessResult: resultExtra must not be null!"
        }

        val resultWrappers = newExtra.resultWrappers ?: emptyList()


        val resultItemsBucketMap: LinkedHashMap<Int, ItemsBucket> = LinkedHashMap()
        itemsBucketIds.forEach {
            val items = mutableListOf<FlatListItem>()
            resultItemsBucketMap[it] = ItemsBucket(it, DEFAULT_ITEMS_TOKEN, items)
        }

        val invokes: MutableList<Invoke> = mutableListOf()

        resultWrappers.forEach { wrapper ->
            var itemsBucket = resultItemsBucketMap[wrapper.itemsBucketId]
            if (itemsBucket == null || itemsBucket.itemsToken != wrapper.itemsToken) {
                val items = mutableListOf<FlatListItem>()
                items.add(wrapper.item)

                itemsBucket = ItemsBucket(wrapper.itemsBucketId, wrapper.itemsToken, items)
                resultItemsBucketMap[wrapper.itemsBucketId] = itemsBucket
            } else {
                (itemsBucket.items as MutableList<FlatListItem>).add(wrapper.item)
            }
        }

        val oldItemsBuckets: List<ItemsBucket> =
            displayedExtra?.resultItemsBucketMap?.values?.toList() ?: emptyList()
        val newItemsBuckets: List<ItemsBucket> =
            resultItemsBucketMap.values.toList()

        val itemsBucketsResult = ListUpdate.calculateDiff(
            oldItemsBuckets,
            newItemsBuckets,
            IElementDiff.BucketDiff()
        )

        itemsBucketsResult.elementOperates.changedElements.forEach { change ->
            invokes.add {

                val cacheItems = IdentityHashMap<FlatListItem, Unit>()
                storage.take(
                    change.second.bucketId,
                    change.second.itemsToken
                )?.items?.forEach {
                    cacheItems[it] = Unit
                }

                // 将当前显示的Item列表标记为未缓存
                val itemsBucket = resultItemsBucketMap[change.second.bucketId]
                val displayedItems = itemsBucket?.items
                displayedItems?.forEach {
                    it.delegate.cached = false
                    cacheItems.remove(it)
                }
                if (cacheItems.isNotEmpty()) {
                    cacheItems.keys.forEach {
                        it.delegate.cached = false
                        it.delegate.onItemDetached()
                    }
                }

                storage.take(
                    change.first.bucketId,
                    change.first.itemsToken
                )?.items?.forEach {
                    it.delegate.cached = false
                    it.delegate.onItemDetached()
                }

                storage.store(change.first)
                change.first.items.forEach {
                    it.delegate.cached = true
                }
            }
        }

        itemsBucketsResult.elementOperates.removedElements.forEach { remove ->
            invokes.add {
                storage.take(
                    remove.bucketId,
                    remove.itemsToken
                )?.items?.forEach {
                    it.delegate.cached = false
                    it.delegate.onItemDetached()
                }
            }
        }

        newExtra.resultItemsBucketMap = resultItemsBucketMap
        newExtra.invokeOnDisplayed = invokes
    }

    override fun onResultDisplayed(displayedData: SourceDisplayedData) {
        super.onResultDisplayed(displayedData)
        (displayedData.resultExtra as? ResultExtra)?.onDisplayed()
    }

    override fun onDetached() {
        super.onDetached()
        storage.clear()
    }

    private fun getItemWrappers(
        itemsBucketIds: List<Int>,
        itemsBucketMap: Map<Int, ItemsBucket>
    ): List<ListItemWrapper> {
        val wrappers = mutableListOf<ListItemWrapper>()
        itemsBucketIds.forEach { id ->
            val itemsBucket = itemsBucketMap[id]
            if (itemsBucket != null) {
                wrappers.addAll(
                    itemsBucket.items.map { ListItemWrapper(id, itemsBucket.itemsToken, it) }
                )
            }
        }
        return wrappers
    }

    sealed class BucketPreShowResult {

        object Unused : BucketPreShowResult()

        class Success() : BucketPreShowResult() {

            private lateinit var _itemsBucketIds: List<Int>
            val itemsBucketIds: List<Int>
                get() = _itemsBucketIds

            private lateinit var _itemsBucketMap: Map<Int, ItemsBucket>
            val itemsBucketMap: Map<Int, ItemsBucket>
                get() = _itemsBucketMap

            constructor(
                itemsBucketIds: List<Int>,
                itemsBucketMap: Map<Int, ItemsBucket>
            ) : this() {
                this._itemsBucketIds = itemsBucketIds
                this._itemsBucketMap = itemsBucketMap
            }
        }

    }

    sealed class BucketLoadResult {

        class Error(
            val error: Throwable
        ) : BucketLoadResult()

        class Success() : BucketLoadResult() {

            private lateinit var _itemsBucketIds: List<Int>
            val itemsBucketIds: List<Int>
                get() = _itemsBucketIds

            private lateinit var _itemsBucketMap: Map<Int, ItemsBucket>
            val itemsBucketMap: Map<Int, ItemsBucket>
                get() = _itemsBucketMap

            constructor(
                itemsBucketIds: List<Int>,
                itemsBucketMap: Map<Int, ItemsBucket>
            ) : this() {
                this._itemsBucketIds = itemsBucketIds
                this._itemsBucketMap = itemsBucketMap
            }
        }
    }

    class ItemsBucketStorage {

        private val cacheMap: MutableMap<Int, MutableMap<Any, ItemsBucket>> = mutableMapOf()

        fun store(bucket: ItemsBucket) {
            var mutableMap = cacheMap[bucket.bucketId]
            if (mutableMap == null) {
                mutableMap = mutableMapOf()
                cacheMap[bucket.bucketId] = mutableMap
            }
            mutableMap[bucket.itemsToken] = bucket
        }

        fun take(bucketId: Int, itemsToken: Any): ItemsBucket? {
            return cacheMap[bucketId]?.remove(itemsToken)
        }

        fun get(bucketId: Int, itemsToken: Any): ItemsBucket? {
            return cacheMap[bucketId]?.get(itemsToken)
        }

        fun clear() {
            val entries = cacheMap.entries
            val iterator = entries.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                next.value.values.forEach { itemsBucket ->
                    itemsBucket.items.forEach {
                        it.delegate.onItemDetached()
                    }
                }
                iterator.remove()
            }
        }
    }

    class ResultExtra {

        var resultItemsBucketMap: LinkedHashMap<Int, ItemsBucket>? = null

        var resultWrappers: List<ListItemWrapper>? = null

        var invokeOnDisplayed: MutableList<Invoke>? = null

        fun onDisplayed() {
            val invokes = this.invokeOnDisplayed
            this.invokeOnDisplayed = null
            invokes?.forEach {
                it()
            }
        }
    }
}

data class ItemsBucket(
    val bucketId: Int,
    val itemsToken: Any,
    val items: List<FlatListItem>
)