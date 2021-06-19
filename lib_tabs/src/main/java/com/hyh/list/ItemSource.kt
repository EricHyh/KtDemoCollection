package com.hyh.list

import com.hyh.RefreshActuator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class ItemSource<Param : Any> {

    internal val delegate: Delegate<Param> =
        object : Delegate<Param>() {

            override fun activate() {
            }

            override fun initPosition(position: Int) {
                _sourcePosition = position
            }

            override fun injectRefreshActuator(refreshActuator: RefreshActuator<Param>) {
                _refreshActuator = refreshActuator
            }

            override fun destroy() {
            }
        }

    private var _sourcePosition: Int = -1
    val sourcePosition: Int
        get() = _sourcePosition

    private lateinit var _refreshActuator: RefreshActuator<Param>
    val refreshActuator: RefreshActuator<Param>
        get() = _refreshActuator


    fun updateItemSource(newPosition: Int, newItemSource: ItemSource<Param>) {
        val oldPosition = _sourcePosition
        _sourcePosition = newPosition
        onUpdateItemSource(oldPosition, newPosition, newItemSource)
    }

    open fun onUpdateItemSource(oldPosition: Int, newPosition: Int, newItemSource: ItemSource<Param>) {}

    abstract suspend fun getPreShow(params: PreShowParams<Param>): PreShowResult
    open suspend fun onPreShowResult(params: PreShowParams<Param>, preShowResult: PreShowResult) {}
    abstract suspend fun load(params: LoadParams<Param>): LoadResult
    open suspend fun onLoadResult(params: LoadParams<Param>, loadResult: LoadResult) {}
    open fun getFetchDispatcher(param: Param): CoroutineDispatcher = Dispatchers.Unconfined

    abstract class Delegate<Param : Any> {
        abstract fun activate()
        abstract fun initPosition(position: Int)
        abstract fun injectRefreshActuator(refreshActuator: RefreshActuator<Param>)
        abstract fun destroy()
    }

    sealed class PreShowResult {

        object Unused : PreShowResult()

        data class Success constructor(
            val items: List<ItemData>
        ) : PreShowResult()
    }

    sealed class LoadResult {

        data class Error(
            val error: Throwable
        ) : LoadResult()

        data class Success constructor(
            val items: List<ItemData>
        ) : LoadResult()
    }

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