package com.hyh.list.adapter

import com.hyh.list.RepoLoadState
import kotlinx.coroutines.flow.Flow

interface IListAdapter {


    val repoLoadStateFlow: Flow<RepoLoadState>

    fun refreshSourceRepo()
    fun refreshAll()
    fun refresh(vararg sourceIndexes: Int)
    fun refresh(vararg sourceTokens: Any)

}