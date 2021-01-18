package com.hyh.paging3demo.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class ProjectRemoteKey(
    @PrimaryKey
    val chapterId: Int,
    val prevPageIndex: Int?,
    val nextPageIndex: Int?
)