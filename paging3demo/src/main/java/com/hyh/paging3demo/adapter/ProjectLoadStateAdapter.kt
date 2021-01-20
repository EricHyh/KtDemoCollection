package com.hyh.paging3demo.adapter

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import com.hyh.paging3demo.ui.NetworkStateItemViewHolder

/**
 * 监听了请求状态的 Adapter.
 *
 * @property adapter 数据列表 Adapter
 */
class ProjectLoadStateAdapter(private val adapter: ProjectAdapter) : LoadStateAdapter<NetworkStateItemViewHolder>() {

    override fun onBindViewHolder(holder: NetworkStateItemViewHolder, loadState: LoadState) {
        holder.bindTo(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): NetworkStateItemViewHolder {
        return NetworkStateItemViewHolder(parent) {
            adapter.retry() //点击Item上的重试按钮
        }
    }
}