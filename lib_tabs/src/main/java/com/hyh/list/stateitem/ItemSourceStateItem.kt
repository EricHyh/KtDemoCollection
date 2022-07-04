package com.hyh.list.stateitem

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.hyh.RefreshActuator
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.coroutine.SingleRunner
import com.hyh.list.*
import com.hyh.list.adapter.getItemSourceLoadState
import com.hyh.list.adapter.getRefreshActuator
import kotlinx.coroutines.flow.collectLatest

/**
 * 监听某个数据源加载状态的Item
 *
 * @author eriche 2022/7/1
 */
abstract class ItemSourceStateItem<VH : RecyclerView.ViewHolder>(
    private val sourceToken: Any
) : IFlatListItem<VH>() {
    companion object {
        private const val TAG = "ItemSourceStateItem"
    }

    private var _itemSourceState: ItemSourceState? = null
    protected val itemSourceState: ItemSourceState?
        get() = _itemSourceState

    private val singleRunner = SingleRunner()

    protected fun getRefreshActuator(viewHolder: VH): RefreshActuator? {
        return viewHolder.getRefreshActuator(sourceToken)
    }

    override fun onBindViewHolder(viewHolder: VH) {
        lifecycleScope.launchWhenStarted {
            singleRunner.runInIsolation {
                getItemSourceLoadStateFlow(viewHolder)?.collectLatest {
                    val state = when (it) {
                        is ItemSourceLoadState.Initial -> {
                            ItemSourceState.Loading(it.currentItemCount)
                        }
                        is ItemSourceLoadState.Loading -> {
                            ItemSourceState.Loading(it.currentItemCount)
                        }
                        is ItemSourceLoadState.PreShow -> {
                            if (it.currentItemCount > 0) {
                                ItemSourceState.Success(it.currentItemCount)
                            } else {
                                ItemSourceState.Loading(it.currentItemCount)
                            }
                        }
                        is ItemSourceLoadState.Success -> {
                            if (it.currentItemCount > 0) {
                                ItemSourceState.Success(it.currentItemCount)
                            } else {
                                ItemSourceState.Empty()
                            }
                        }
                        is ItemSourceLoadState.Error -> {
                            if (it.currentItemCount > 0) {
                                ItemSourceState.Success(it.currentItemCount)
                            } else {
                                ItemSourceState.Error(it.currentItemCount)
                            }
                        }
                    }
                    _itemSourceState = state
                    bindPageState(viewHolder, state)
                }
            }
        }
    }

    abstract fun bindPageState(viewHolder: VH, state: ItemSourceState)

    override fun areContentsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is ItemSourceStateItem) return false
        return sourceToken == newItem.sourceToken
    }

    override fun areItemsTheSame(newItem: FlatListItem): Boolean {
        if (newItem !is ItemSourceStateItem) return false
        return sourceToken == newItem.sourceToken
    }

    private fun getItemSourceLoadStateFlow(viewHolder: VH): SimpleStateFlow<ItemSourceLoadState>? {
        return viewHolder.getItemSourceLoadState(sourceToken)
    }
}


sealed class ItemSourceState(open val currentItemCount: Int) {

    data class Loading(override val currentItemCount: Int) : ItemSourceState(currentItemCount)
    data class Success(override val currentItemCount: Int) : ItemSourceState(currentItemCount)
    data class Error(override val currentItemCount: Int) : ItemSourceState(currentItemCount)
    data class Empty(override val currentItemCount: Int = 0) : ItemSourceState(currentItemCount)

}