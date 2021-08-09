package com.hyh.list

import com.hyh.Invoke
import com.hyh.list.internal.*

abstract class ItemsBucketSource<Param : Any> : ItemSource<Param, ListItemWrapper>() {

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

    override fun getElementDiff(): IElementDiff<ListItemWrapper> {
        return IElementDiff.ItemDataWrapperDiff()
    }

    override fun mapItems(items: List<ListItemWrapper>): List<FlatListItem> {
        return items.map { it.item }
    }

    override fun onItemsDisplayed(items: List<ListItemWrapper>) {
        items.forEach {
            if (!it.attached) {
                it.item.delegate.onItemAttached()
            }
            it.item.delegate.onItemActivated()
        }
    }

    override fun onItemsChanged(changes: List<Triple<ListItemWrapper, ListItemWrapper, Any?>>) {
        changes.forEach {
            it.first.item.delegate.updateItem(it.second.item, it.third)
        }
    }

    override fun onItemsRecycled(items: List<ListItemWrapper>) {
        items.forEach {
            it.item.delegate.onItemInactivated()
            if (!it.cached) {
                it.item.delegate.onItemDetached()
            }
        }
    }

    override suspend fun getPreShow(params: PreShowParams<Param, ListItemWrapper>): PreShowResult<ListItemWrapper> {
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

    override suspend fun load(params: LoadParams<Param, ListItemWrapper>): LoadResult<ListItemWrapper> {
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

    protected abstract suspend fun getPreShow(param: Param, displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?): BucketPreShowResult
    protected abstract suspend fun load(param: Param, displayedItemsBucketMap: LinkedHashMap<Int, ItemsBucket>?): BucketLoadResult


    override fun onProcessResult(
        resultItems: List<ListItemWrapper>,
        resultExtra: Any?,
        displayedData: SourceDisplayedData<ListItemWrapper>
    ) {
        val displayedExtra = displayedData.resultExtra as? ResultExtra
        val newExtra = resultExtra as? ResultExtra
        check(newExtra != null) {
            "$this onProcessResult: resultExtra must not be null!"
        }
        val resultItemsBucketMap: LinkedHashMap<Int, ItemsBucket> = LinkedHashMap()
        itemsBucketIds.forEach {
            val items = mutableListOf<FlatListItem>()
            resultItemsBucketMap[it] = ItemsBucket(it, DEFAULT_ITEMS_TOKEN, items)
        }

        resultItems.forEach { wrapper ->
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

        val invokes: MutableList<Invoke> = mutableListOf()


        itemsBucketsResult.elementOperates.changedElements.forEach { change ->
            invokes.add {
                storage.take(
                    change.first.bucketId,
                    change.first.itemsToken
                )?.items?.forEach {
                    it.delegate.cached = false
                }

                storage.store(change.first)

                change.first.items.forEach {
                    it.delegate.cached = true
                }
            }
        }

        newExtra.resultItemsBucketMap = resultItemsBucketMap
        newExtra.invokeOnDisplayed = invokes
    }

    override fun onResultDisplayed(displayedData: SourceDisplayedData<ListItemWrapper>) {
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