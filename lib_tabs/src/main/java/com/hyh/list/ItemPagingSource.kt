package com.hyh.list

import com.hyh.AppendActuator
import com.hyh.RearrangeActuator
import com.hyh.list.internal.base.BaseItemSource
import com.hyh.list.internal.PagingSourceDisplayedData

/**
 * 支持加载更多的ItemSource
 *
 * @param Param
 * @param Item
 * @property initialParam
 */
abstract class ItemPagingSource<Param : Any, Item : Any>(val initialParam: Param?) : BaseItemSource<ItemPagingSource.LoadParams<Param>, Item>() {


    inner class PagingSourceDelegate : DefaultDelegate() {

        fun injectAppendActuator(appendActuator: AppendActuator) {
            _appendActuator = appendActuator
        }

        fun injectRearrangeActuator(rearrangeActuator: RearrangeActuator) {
            _rearrangeActuator = rearrangeActuator
        }
    }

    override val delegate: PagingSourceDelegate = PagingSourceDelegate()

    private var _appendActuator: AppendActuator? = null
    val appendActuator: AppendActuator = {
        _appendActuator?.invoke(it)
    }

    private var _rearrangeActuator: RearrangeActuator? = null
    val rearrangeActuator: RearrangeActuator = {
        _rearrangeActuator?.invoke(it)
    }

    abstract suspend fun load(params: LoadParams<Param>): LoadResult<Param, Item>

    abstract suspend fun getRefreshKey(): Param?

    sealed class LoadParams<Param : Any> {

        abstract val param: Param?

        class Refresh<Param : Any>(override val param: Param?) : LoadParams<Param>()

        class Append<Param : Any>(override val param: Param?) : LoadParams<Param>()

        class Rearrange<Param : Any>(
            val displayedData: PagingSourceDisplayedData<Param>
        ) : LoadParams<Param>() {
            override val param: Param? = null
        }
    }

    sealed class LoadResult<Param : Any, Item : Any> {

        data class Error<Param : Any, Item : Any>(
            val throwable: Throwable
        ) : LoadResult<Param, Item>()

        data class Success<Param : Any, Item : Any>(
            val items: List<Item>,
            val nextParam: Param?,
            val noMore: Boolean = false,
            val resultExtra: Any? = null
        ) : LoadResult<Param, Item>()

        data class Rearranged<Param : Any, Item : Any>(
            val ignore: Boolean = true,
            val items: List<Item> = emptyList(),
            val resultExtra: Any? = null
        ) : LoadResult<Param, Item>()
    }
}