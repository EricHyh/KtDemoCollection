package com.hyh.list.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hyh.list.ItemData
import com.hyh.list.RepoLoadState
import com.hyh.list.SourceLoadState
import com.hyh.list.internal.RepoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IListAdapter<Param : Any> {

    val repoLoadStateFlow: StateFlow<RepoLoadState>

    fun submitData(flow: Flow<RepoData<Param>>)

    fun getSourceLoadState(sourceIndex: Int): StateFlow<SourceLoadState>?
    fun getSourceLoadState(sourceToken: Any): StateFlow<SourceLoadState>?

    fun getItemSnapshot(): List<ItemData>
    fun getItemSnapshot(sourceIndexStart: Int, count: Int = 1): List<ItemData>
    fun getItemSnapshot(sourceTokenStart: Any, count: Int = 1): List<ItemData>


    fun findItemLocalInfo(view: View, recyclerView: RecyclerView): ItemLocalInfo?


    fun refreshRepo(param: Param)

    fun refreshSources()
    fun refreshSources(vararg sourceIndexes: Int)
    fun refreshSources(sourceIndexStart: Int, count: Int)
    fun refreshSources(vararg sourceTokens: Any)
    fun refreshSources(sourceTokenStart: Any, count: Int)

}

data class ItemLocalInfo(
    val sourceToken: Any,
    val localPosition: Int,
    val sourceItemCount: Int
)