package com.hyh.list.state

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.hyh.RefreshActuator
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.coroutine.SingleRunner
import com.hyh.list.*
import com.hyh.list.adapter.getItemSourceLoadState
import com.hyh.list.adapter.getRefreshActuator
import com.hyh.page.state.PageState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    private val singleRunner = SingleRunner()

    protected fun getRefreshActuator(viewHolder: VH): RefreshActuator? {
        return viewHolder.getRefreshActuator(sourceToken)
    }

    override fun onBindViewHolder(viewHolder: VH) {
        lifecycleScope.launch {
            singleRunner.runInIsolation {
                getItemSourceLoadStateFlow(viewHolder)?.collectLatest {
                    val state = when (it) {
                        is ItemSourceLoadState.Initial -> {
                            PageState.LOADING
                        }
                        is ItemSourceLoadState.Loading -> {
                            PageState.LOADING
                        }
                        is ItemSourceLoadState.PreShow -> {
                            if (it.itemCount > 0) {
                                PageState.SUCCESS
                            } else {
                                PageState.LOADING
                            }
                        }
                        is ItemSourceLoadState.Success -> {
                            if (it.itemCount > 0) {
                                PageState.SUCCESS
                            } else {
                                PageState.EMPTY
                            }
                        }
                        is ItemSourceLoadState.Error -> {
                            if (it.currentItemCount > 0) {
                                PageState.SUCCESS
                            } else {
                                PageState.ERROR
                            }
                        }
                    }
                    bindPageState(viewHolder, state)
                }
            }
        }
    }

    abstract fun bindPageState(viewHolder: VH, state: PageState)

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