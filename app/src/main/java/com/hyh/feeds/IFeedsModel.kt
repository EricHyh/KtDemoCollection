package com.hyh.feeds

import kotlinx.coroutines.flow.Flow

interface IFeedsModel {

    fun getCache(): Flow<FeedsResult>

    fun refresh(): Flow<FeedsResult>

    fun loadMore(): Flow<FeedsResult>

}

data class FeedsResult(val code: Int, val items: List<IItemData>) {

    companion object {

        const val CODE_SUCCESS = 0
        const val CODE_EMPTY = 1
        const val CODE_ERROR = 2

    }
}