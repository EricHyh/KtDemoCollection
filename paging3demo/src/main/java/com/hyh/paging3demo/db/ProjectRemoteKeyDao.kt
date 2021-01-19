package com.hyh.paging3demo.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hyh.paging3demo.bean.ProjectRemoteKey

@Dao
interface ProjectRemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: ProjectRemoteKey)

    @Query("SELECT * FROM remote_keys WHERE chapterId = :chapterId")
    suspend fun getRemoteKey(chapterId: Int): ProjectRemoteKey

    @Query("DELETE FROM remote_keys WHERE chapterId = :chapterId")
    suspend fun delete(chapterId: Int)
}