package com.hyh.list.state

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.hyh.AppendActuator
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.coroutine.SingleRunner
import com.hyh.list.*
import com.hyh.list.adapter.getAppendActuator
import com.hyh.list.adapter.getPagingSourceLoadState
import kotlinx.coroutines.flow.collectLatest

/**
 * 监听加载更多状态的Item
 *
 * @author eriche 2022/7/1
 */
abstract class AppendStateItem<VH : RecyclerView.ViewHolder>(
    private val pagingSourceToken: Any
) : IFlatListItem<VH>() {
    companion object {
        private const val TAG = "ItemSourceStateItem"
    }

    private var _appendState: AppendState? = null
    protected val appendState: AppendState?
        get() = _appendState

    private val singleRunner = SingleRunner()

    protected fun getAppendActuator(viewHolder: VH): AppendActuator? {
        return viewHolder.getAppendActuator(pagingSourceToken)
    }

    override fun onBindViewHolder(viewHolder: VH) {
        lifecycleScope.launchWhenStarted {
            singleRunner.runInIsolation {
                getPagingSourceLoadStateFlow(viewHolder)?.collectLatest {
                    val state = when (it) {
                        is PagingSourceLoadState.Initial -> {
                            AppendState.Loading
                        }
                        is PagingSourceLoadState.Refreshing -> {
                            AppendState.Loading
                        }
                        is PagingSourceLoadState.RefreshSuccess -> {
                            if (it.endOfPaginationReached) {
                                AppendState.NoMore
                            } else {
                                AppendState.Loading
                            }
                        }
                        is PagingSourceLoadState.RefreshError -> {
                            AppendState.Error
                        }
                        is PagingSourceLoadState.Appending -> {
                            AppendState.Loading
                        }
                        is PagingSourceLoadState.AppendError -> {
                            AppendState.Error
                        }
                        is PagingSourceLoadState.AppendSuccess -> {
                            if (it.endOfPaginationReached) {
                                AppendState.NoMore
                            } else {
                                AppendState.Success
                            }
                        }
                    }
                    _appendState = state
                    bindAppendState(viewHolder, state)
                }
            }
        }
    }

    override fun onItemInactivated() {
        super.onItemInactivated()
        _appendState = null
    }

    abstract fun bindAppendState(viewHolder: VH, state: AppendState)

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is AppendStateItem) return false
        return pagingSourceToken == newItem.pagingSourceToken
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is AppendStateItem) return false
        return pagingSourceToken == newItem.pagingSourceToken
    }

    private fun getPagingSourceLoadStateFlow(viewHolder: VH): SimpleStateFlow<PagingSourceLoadState>? {
        return viewHolder.getPagingSourceLoadState(pagingSourceToken)
    }
}


enum class AppendState {

    Loading,
    Success,
    Error,
    NoMore,

}