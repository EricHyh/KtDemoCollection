package com.hyh.list.internal

import kotlinx.coroutines.flow.Flow

data class RepoData(
    val flow: Flow<RepoEvent>,
    val receiver: UiReceiverForRepo
)

data class SourceData<Param : Any>(
    val sourceToken: Any,
    val flow: Flow<SourceEvent>,
    val receiver: UiReceiverForSource<Param>
)
