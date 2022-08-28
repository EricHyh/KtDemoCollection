package com.hyh.list

import com.hyh.AppendActuator
import com.hyh.Invoke
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
abstract class ItemPagingSource<Param, Item>(open var initialParam: Param?) :
    BaseItemSource<ItemPagingSource.LoadParams<Param>, Item>() {


    inner class PagingSourceDelegate : DefaultDelegate() {

        fun injectAppendActuator(appendActuator: AppendActuator) {
            _appendActuator = appendActuator
        }

        fun injectRearrangeActuator(rearrangeActuator: RearrangeActuator) {
            _rearrangeActuator = rearrangeActuator
        }
    }

    override val delegate: PagingSourceDelegate = PagingSourceDelegate()

    /**
     * 加载更多执行器
     */
    private var _appendActuator: AppendActuator? = null
    val appendActuator: AppendActuator = {
        _appendActuator?.invoke(it)
    }

    /**
     * 数据重排执行器
     */
    private var _rearrangeActuator: RearrangeActuator? = null
    val rearrangeActuator: RearrangeActuator = {
        _rearrangeActuator?.invoke(it)
    }

    abstract suspend fun load(params: LoadParams<Param>): LoadResult<Param, Item>

    abstract suspend fun getRefreshKey(): Param?

    sealed class LoadParams<Param> {

        abstract val param: Param?

        /**
         * 刷新请求
         */
        class Refresh<Param>(override val param: Param?) : LoadParams<Param>()

        /**
         * 加载更多请求
         */
        class Append<Param>(override val param: Param?) : LoadParams<Param>()

        /**
         * 数据重排请求
         */
        class Rearrange<Param>(
            val displayedData: PagingSourceDisplayedData<Param>
        ) : LoadParams<Param>() {
            override val param: Param? = null
        }
    }

    sealed class LoadResult<Param, Item> {

        /**
         * 请求失败时返回
         */
        data class Error<Param, Item>(
            /**
             * 失败异常信息
             */
            val throwable: Throwable
        ) : LoadResult<Param, Item>()


        /**
         * 请求成功[LoadParams.Refresh]、[LoadParams.Append]时返回
         */
        data class Success<Param, Item>(
            /**
             * 请求成功的列表数据：
             *
             * 1.如果是刷新，则返回刷新数据
             * 2.如果是加载更多，则返回加载更多那一部分数据（不是全量数据）
             *
             */
            val items: List<Item>,
            /**
             * 请求下一页的参数，在请求下一页时，会传递给[load]函数
             */
            val nextParam: Param?,
            /**
             * 是否没有更多数据，如果为true，那么就不会在继续请求下一页数据
             */
            val noMore: Boolean = false,
            /**
             * 额为数据，由业务自定义，会保存在[BaseItemSource.displayedData]中
             */
            val resultExtra: Any? = null,
            /**
             * 当结果被使用时回调
             */
            val onResultDisplayed: Invoke? = null
        ) : LoadResult<Param, Item>()


        /**
         * 数据重排[LoadParams.Rearrange]时返回
         */
        data class Rearranged<Param, Item>(
            /**
             * 是否忽略这次数据重排请求，不作任何操作
             */
            val ignore: Boolean = true,
            /**
             * 数据重排后的全量数据
             */
            val items: List<Item> = emptyList(),
            /**
             * 额为数据，由业务自定义，会保存在[BaseItemSource.displayedData]中
             */
            val resultExtra: Any? = null,
            /**
             * 当结果被使用时回调
             */
            val onResultDisplayed: Invoke? = null
        ) : LoadResult<Param, Item>()
    }
}