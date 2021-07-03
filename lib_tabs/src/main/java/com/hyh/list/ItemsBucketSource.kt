package com.hyh.list

import com.hyh.Invoke
import com.hyh.list.internal.*

abstract class ItemsBucketSource<Param : Any> : ItemSource<Param, ItemDataWrapper>() {

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

    override fun getElementDiff(): IElementDiff<ItemDataWrapper> {
        return IElementDiff.ItemDataWrapperDiff()
    }

    override fun mapItems(items: List<ItemDataWrapper>): List<ItemData> {
        return items.map { it.itemData }
    }

    override fun onItemsDisplayed(items: List<ItemDataWrapper>) {
        items.forEach {
            if (!it.attached) {
                it.itemData.delegate.onAttached()
            }
            it.itemData.delegate.onActivated()
        }
    }

    override fun onItemsChanged(changes: List<Triple<ItemDataWrapper, ItemDataWrapper, Any?>>) {
        changes.forEach {
            it.first.itemData.delegate.updateItemData(it.second.itemData, it.third)
        }
    }

    override fun onItemsRecycled(items: List<ItemDataWrapper>) {
        items.forEach {
            it.itemData.delegate.onInactivated()
            if (!it.cached) {
                it.itemData.delegate.onDetached()
            }
        }
    }

    override suspend fun getPreShow(params: PreShowParams<Param, ItemDataWrapper>): PreShowResult<ItemDataWrapper> {
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

    override suspend fun load(params: LoadParams<Param, ItemDataWrapper>): LoadResult<ItemDataWrapper> {
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
        resultItems: List<ItemDataWrapper>,
        resultExtra: Any?,
        displayedData: SourceDisplayedData<ItemDataWrapper>
    ) {
        val displayedExtra = displayedData.resultExtra as? ResultExtra
        val newExtra = resultExtra as? ResultExtra
        check(newExtra != null) {
            "$this onProcessResult: resultExtra must not be null!"
        }
        val resultItemsBucketMap: LinkedHashMap<Int, ItemsBucket> = LinkedHashMap()
        itemsBucketIds.forEach {
            val items = mutableListOf<ItemData>()
            resultItemsBucketMap[it] = ItemsBucket(it, DEFAULT_ITEMS_TOKEN, items)
        }

        resultItems.forEach { wrapper ->
            var itemsBucket = resultItemsBucketMap[wrapper.itemsBucketId]
            if (itemsBucket == null || itemsBucket.itemsToken != wrapper.itemsToken) {
                val items = mutableListOf<ItemData>()
                items.add(wrapper.itemData)

                itemsBucket = ItemsBucket(wrapper.itemsBucketId, wrapper.itemsToken, items)
                resultItemsBucketMap[wrapper.itemsBucketId] = itemsBucket
            } else {
                (itemsBucket.items as MutableList<ItemData>).add(wrapper.itemData)
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

        itemsBucketsResult.elementOperates.forEach { operate ->
            if (operate is ElementOperate.Changed<ItemsBucket>) {
                invokes.add {
                    storage.take(
                        operate.oldElement.bucketId,
                        operate.oldElement.itemsToken
                    )?.items?.forEach {
                        it.delegate.cached = false
                    }

                    storage.store(operate.oldElement)

                    operate.oldElement.items.forEach {
                        it.delegate.cached = true
                    }
                }
            }
        }

        newExtra.resultItemsBucketMap = resultItemsBucketMap
        newExtra.invokeOnDisplayed = invokes
    }

    override fun onResultDisplayed(displayedData: SourceDisplayedData<ItemDataWrapper>) {
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
    ): List<ItemDataWrapper> {
        val wrappers = mutableListOf<ItemDataWrapper>()
        itemsBucketIds.forEach { id ->
            val itemsBucket = itemsBucketMap[id]
            if (itemsBucket != null) {
                wrappers.addAll(
                    itemsBucket.items.map { ItemDataWrapper(id, itemsBucket.itemsToken, it) }
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
                        it.delegate.onDetached()
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