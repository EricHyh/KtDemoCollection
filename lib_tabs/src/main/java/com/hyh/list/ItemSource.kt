package com.hyh.list

import com.hyh.RefreshActuator
import com.hyh.list.internal.RefreshStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class ItemSource<Param : Any> {

    companion object {
        const val DEFAULT_ITEMS_BUCKET_ID = -1
        val NONE_TOKEN = Any()
    }

    internal val delegate: Delegate<Param> = object : Delegate<Param>() {

        override fun activate() {
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

        override fun destroy() {
            _sourcePosition = -1
        }
    }

    private var _sourcePosition: Int = -1
    val sourcePosition: Int
        get() = _sourcePosition

    private lateinit var _refreshActuator: RefreshActuator
    val refreshActuator: RefreshActuator
        get() = _refreshActuator

    private var itemsBucketIds: List<Int> = listOf(DEFAULT_ITEMS_BUCKET_ID)

    open fun onUpdateItemSource(oldPosition: Int, newPosition: Int, newItemSource: ItemSource<Param>) {}

    open fun getRefreshStrategy(): RefreshStrategy = RefreshStrategy.CancelLast
    abstract suspend fun getParam(): Param
    abstract suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult
    open suspend fun onPreShowResult(params: PreShowParams<Param>, preShowResult: PreShowResult) {}
    abstract suspend fun load(params: LoadParams<Param>): LoadResult
    open suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {}
    open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined


    protected fun registerItemsBucketIds(itemsBucketIds: List<Int>) {

    }

    protected fun markItems(items: List<ItemData>, itemsToken: Any) {

    }

    abstract class Delegate<Param : Any> {

        abstract fun activate()
        abstract fun initPosition(position: Int)
        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator)
        abstract fun updateItemSource(newPosition: Int, newItemSource: ItemSource<Param>)
        abstract fun onBucketItemsTokenChanged(
            bucketId: Int,
            oldItemsToken: Any,
            oldItems: List<ItemData>,
            newItemsToken: Any,
            newItems: List<ItemData>
        )

        abstract fun destroy()
    }

    interface ItemsStorage {

        fun store(itemsToken: Any, items: List<ItemData>)

        fun isStored(itemsToken: Any): Boolean

        fun take(itemsToken: Any): List<ItemData>

        fun get(itemsToken: Any): List<ItemData>

        fun remove(itemsToken: Any)

    }

    sealed class PreShowResult {

        object Unused : PreShowResult()

        data class Success constructor(
            val items: List<ItemData>,
            val itemsBucketIds: List<Int>,
            val itemsBuckets: List<ItemsBucket>
        ) : PreShowResult()
    }

    sealed class LoadResult {

        data class Error(
            val error: Throwable
        ) : LoadResult()

        data class Success constructor(
            val items: List<ItemData>,
            val itemsBucketIds: List<Int>,
            val itemsBuckets: List<ItemsBucket>
        ) : LoadResult()
    }

    data class ItemsBucket(
        val bucketId: Int,
        val itemsToken: Any,
        val items: List<ItemData>,
    )

    class PreShowParams<Param : Any>(
        val param: Param,
        val displayedItemsSnapshot: List<ItemData>?,
        val lastPreShowResult: PreShowResult?,
        val lastLoadResult: LoadResult?
    )

    class LoadParams<Param : Any>(
        val param: Param,
        val displayedItemsSnapshot: List<ItemData>?,
        val lastPreShowResult: PreShowResult?,
        val lastLoadResult: LoadResult?
    )
}