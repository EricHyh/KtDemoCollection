package com.hyh.list

import com.hyh.RefreshActuator
import com.hyh.base.LoadStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class ItemSource<Param : Any> {

    companion object {
        const val DEFAULT_ITEMS_BUCKET_ID = -1
        val DEFAULT_ITEMS_TOKEN = Any()
    }

    internal val delegate: Delegate<Param> = object : Delegate<Param>() {

        override fun attach() {
            super.attach()
        }

        override fun initPosition(position: Int) {
            _sourcePosition = position
        }

        override fun injectRefreshActuator(refreshActuator: RefreshActuator) {
            _refreshActuator = refreshActuator
        }

        override fun updateItemSource(newPosition: Int, newItemSource: ItemSource<Param>) {
            val oldPosition = _sourcePosition
            _sourcePosition = newPosition
            onUpdateItemSource(oldPosition, newPosition, newItemSource)
        }

        override fun detach() {
            super.detach()
            _sourcePosition = -1
        }

        override fun onBucketAdded(bucket: ItemsBucket) {}

        override fun shouldCacheBucket(itemsBucket: ItemsBucket): Boolean {
            return this@ItemSource.shouldCacheBucket(itemsBucket)
        }

        override fun onBucketRemoved(bucket: ItemsBucket) {}

    }

    private var _sourcePosition: Int = -1
    val sourcePosition: Int
        get() = _sourcePosition

    private lateinit var _refreshActuator: RefreshActuator
    val refreshActuator: RefreshActuator
        get() = _refreshActuator

    private var _itemsBucketIds: List<Int> = listOf(DEFAULT_ITEMS_BUCKET_ID)
    protected val itemsBucketIds: List<Int>
        get() = _itemsBucketIds

    protected open fun onAttached() {}

    open fun onUpdateItemSource(oldPosition: Int, newPosition: Int, newItemSource: ItemSource<Param>) {}
    open fun getLoadStrategy(): LoadStrategy = LoadStrategy.CancelLast
    abstract suspend fun getParam(): Param
    abstract suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult
    open suspend fun onPreShowResult(params: PreShowParams<Param>, preShowResult: PreShowResult) {}
    abstract suspend fun load(params: LoadParams<Param>): LoadResult
    open suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {}

    protected open fun shouldCacheBucket(itemsBucket: ItemsBucket) = false

    open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    protected open fun onDetached() {}

    protected fun registerItemsBucketIds(itemsBucketIds: List<Int>) {
        this._itemsBucketIds = itemsBucketIds
    }

    abstract class Delegate<Param : Any> {

        val storage: ItemsBucketStorage = object : ItemsBucketStorage {

            private val cacheMap: MutableMap<Int, MutableMap<Any, ItemsBucket>> = mutableMapOf()

            override fun store(bucket: ItemsBucket) {
                var mutableMap = cacheMap[bucket.bucketId]
                if (mutableMap == null) {
                    mutableMap = mutableMapOf()
                    cacheMap[bucket.bucketId] = mutableMap
                }
                mutableMap[bucket.itemsToken] = bucket
            }

            override fun take(bucketId: Int, itemsToken: Any): ItemsBucket? {
                return cacheMap[bucketId]?.remove(itemsToken)
            }

            override fun get(bucketId: Int, itemsToken: Any): ItemsBucket? {
                return cacheMap[bucketId]?.get(itemsToken)
            }

            override fun clear() {
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

        open fun attach() {}

        abstract fun initPosition(position: Int)
        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator)
        abstract fun updateItemSource(newPosition: Int, newItemSource: ItemSource<Param>)
        abstract fun onBucketAdded(bucket: ItemsBucket)
        abstract fun onBucketRemoved(bucket: ItemsBucket)
        abstract fun shouldCacheBucket(itemsBucket: ItemsBucket): Boolean

        open fun detach() {
            storage.clear()
        }

    }

    interface ItemsBucketStorage {

        fun store(bucket: ItemsBucket)

        fun take(bucketId: Int, itemsToken: Any): ItemsBucket?

        fun get(bucketId: Int, itemsToken: Any): ItemsBucket?

        fun clear()

    }

    sealed class PreShowResult {

        object Unused : PreShowResult()

        class Success() : PreShowResult() {

            private lateinit var _itemsBucketIds: List<Int>
            val itemsBucketIds: List<Int>
                get() = _itemsBucketIds

            private lateinit var _itemsBucketMap: Map<Int, ItemsBucket>
            val itemsBucketMap: Map<Int, ItemsBucket>
                get() = _itemsBucketMap

            constructor(items: List<ItemData>) : this() {
                this._itemsBucketIds = listOf(DEFAULT_ITEMS_BUCKET_ID)
                this._itemsBucketMap = mapOf(
                    DEFAULT_ITEMS_BUCKET_ID to ItemsBucket(DEFAULT_ITEMS_BUCKET_ID, DEFAULT_ITEMS_TOKEN, items)
                )
            }

            constructor(itemsBucketIds: List<Int>, itemsBucketMap: Map<Int, ItemsBucket>) : this() {
                this._itemsBucketIds = itemsBucketIds
                this._itemsBucketMap = itemsBucketMap
            }
        }
    }

    sealed class LoadResult {

        class Error(
            val error: Throwable
        ) : LoadResult()

        class Success() : LoadResult() {

            private lateinit var _itemsBucketIds: List<Int>
            val itemsBucketIds: List<Int>
                get() = _itemsBucketIds

            private lateinit var _itemsBucketMap: Map<Int, ItemsBucket>
            val itemsBucketMap: Map<Int, ItemsBucket>
                get() = _itemsBucketMap

            constructor(items: List<ItemData>) : this() {
                this._itemsBucketIds = listOf(DEFAULT_ITEMS_BUCKET_ID)
                this._itemsBucketMap = mapOf(
                    DEFAULT_ITEMS_BUCKET_ID to ItemsBucket(DEFAULT_ITEMS_BUCKET_ID, DEFAULT_ITEMS_TOKEN, items)
                )
            }

            constructor(itemsBucketIds: List<Int>, itemsBucketMap: Map<Int, ItemsBucket>) : this() {
                this._itemsBucketIds = itemsBucketIds
                this._itemsBucketMap = itemsBucketMap
            }
        }
    }

    data class ItemsBucket(
        val bucketId: Int,
        val itemsToken: Any,
        val items: List<ItemData>,
    )

    class PreShowParams<Param : Any>(
        val param: Param,
        var displayedItemsBucketMap: Map<Int, ItemsBucket>?,
        val displayedItems: List<ItemData>?,
        val lastPreShowResult: PreShowResult?,
        val lastLoadResult: LoadResult?
    )

    class LoadParams<Param : Any>(
        val param: Param,
        var displayedItemsBucketMap: Map<Int, ItemsBucket>?,
        val displayedItems: List<ItemData>?,
        val lastPreShowResult: PreShowResult?,
        val lastLoadResult: LoadResult?
    )
}