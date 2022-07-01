package com.hyh.list.internal.paging

sealed class LoadEvent {

    object Refresh : LoadEvent()

    object Append : LoadEvent()

    object Rearrange : LoadEvent()

}