package com.hyh.list.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hyh.coroutine.SimpleStateFlow
import com.hyh.list.*
import com.hyh.list.internal.IFetcher
import com.hyh.list.internal.RepoData
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

interface IListAdapter<Param> : IFetcher<Param> {

    val repoLoadStateFlow: SimpleStateFlow<RepoLoadState>

    val sourceLoadStatesFlow: SimpleStateFlow<SourceLoadStates>

    val sourceTokens: List<Any>

    fun getItemSourceLoadState(sourceToken: Any): SimpleStateFlow<ItemSourceLoadState>?
    fun getPagingSourceLoadState(sourceToken: Any): SimpleStateFlow<PagingSourceLoadState>?

    fun submitData(flow: Flow<RepoData<Param>>)

    fun getItem(globalPosition: Int): FlatListItem? = findItemLocalInfo(globalPosition)?.item

    fun getItemSnapshot(): List<FlatListItem>
    fun getItemSnapshot(sourceIndexStart: Int, count: Int = 1): List<FlatListItem>
    fun getItemSnapshot(sourceTokenStart: Any, count: Int = 1): List<FlatListItem>

    fun indexOf(sourceToken: Any): Int {
        return sourceTokens.indexOf(sourceToken)
    }

    fun findItemLocalInfo(globalPosition: Int): ItemLocalInfo?
    fun findItemLocalInfo(view: View, recyclerView: RecyclerView): ItemLocalInfo?

    fun moveGlobalItem(from: Int, to: Int): Boolean {
        if (from < 0 || to < 0) return false
        val itemLocalInfo = findItemLocalInfo(from) ?: return false
        if (from == to) return true
        if (abs(to - from) >= itemLocalInfo.sourceItemCount) return false
        val localFrom = itemLocalInfo.localPosition
        val localTo = localFrom + (to - from)
        return moveSourceItem(itemLocalInfo.sourceToken, localFrom, localTo)
    }

    fun moveSourceItem(sourceIndex: Int, from: Int, to: Int): Boolean

    fun moveSourceItem(sourceToken: Any, from: Int, to: Int): Boolean

    fun removeItem(sourceToken: Any, position: Int, count: Int = 1)

    fun removeItem(sourceToken: Any, item: FlatListItem)

    fun insertItems(sourceToken: Any, position: Int, items: List<FlatListItem>)

}

data class ItemLocalInfo constructor(
    val globalPosition: Int,
    val sourceToken: Any,
    val localPosition: Int,
    val sourceItemCount: Int,
    val item: FlatListItem
)